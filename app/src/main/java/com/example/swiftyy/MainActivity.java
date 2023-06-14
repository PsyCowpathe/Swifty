package com.example.swiftyy;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebViewClient;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.squareup.picasso.Picasso;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HttpsURLConnection;


public class MainActivity extends AppCompatActivity
{
    private TextView    login;
    private WebView     webView;
    private String      temporaryToken;
    private String      finalToken = null;
    private String      refreshToken = null;
    private String      returnedState = null;
    private JsonObject  currentUser = null;
    private JsonObject  findUser = null;
    private JsonObject  userCursus = null;
    private JsonArray   userProject = null;
    private boolean     isFind = false;

    private Integer     count = 0;


    //===========       APP_STATE         ==========

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    //===========       ON_CLICk         ==========

    public void searchLogin(View v)
    {
        ExecutorService findService = Executors.newFixedThreadPool(1);

        findService.execute(new Runnable()
        {
            public void run()
            {
                login = findViewById(R.id.LoginForm);
                System.out.println("Searching user : " + login.getText().toString());
                if (login.getText() != null)
                    getStudentInfo(login.getText().toString());
                System.out.println("User info obtained = " + findUser);
                makeSleep(500);
                if (findUser != null)
                {
                    getStudentCursus(findUser.get("id").getAsString());
                    System.out.println("StudentCursus info obtained = " + userCursus);
                    makeSleep(500);
                    getStudentProject(findUser.get("id").getAsString());
                    System.out.println("StudentProject info obtained = " + userProject);
                    if (userProject != null)
                        isFind = true;
                }
            }
        });
        findService.shutdown();
        try
        {
            if (!findService.awaitTermination(60, TimeUnit.SECONDS))
                findService.shutdownNow();
            setContentView(R.layout.logged);
            if (isFind)
                showUserProfile(findUser);
            count = 0;
        }
        catch (InterruptedException ex)
        {
            findService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void logout(View v)
    {
        android.webkit.CookieManager cookieManager = CookieManager.getInstance();

        temporaryToken = null;
        finalToken = null;
        cookieManager.removeAllCookies(null);
        setContentView(R.layout.activity_main);
    }
    public void openWeb(View v)
    {
        Context context = this;
        String  originalState = randomString(35);


        setContentView(R.layout.webview);
        webView = (WebView) findViewById(R.id.webView1);
        String Url = (context.getResources().getString(R.string.login_url) + originalState).toString();
        webView.setWebViewClient(new WebViewClient()
        {
            @Override
            public void onPageFinished(WebView view, String Url)
            {
                final String newUrl = webView.getUrl().toString();
                ExecutorService tokenService = Executors.newFixedThreadPool(5);
                ExecutorService userService = Executors.newFixedThreadPool(5);

                if (newUrl.startsWith(context.getResources().getString(R.string.uri))
                        && newUrl.contains("code") && newUrl.contains("state"))
                {
                    Uri uri = Uri.parse(webView.getUrl());
                    temporaryToken = uri.getQueryParameter("code");
                    returnedState = uri.getQueryParameter("state");
                    if (!returnedState.equals(originalState))
                    {
                        setContentView(R.layout.activity_main);
                        Toast toast = Toast.makeText(context, R.string.error_csrf, Toast.LENGTH_LONG);
                        toast.show();
                        return ;
                    }
                    tokenService.execute(new Runnable()
                    {
                        public void run()
                        {
                            getFinalToken();
                        }
                    });
                    tokenService.shutdown();
                    try
                    {
                        if (!tokenService.awaitTermination(60, TimeUnit.SECONDS))
                            tokenService.shutdownNow();
                        if (finalToken != null)
                        {
                            userService.execute(new Runnable()
                            {
                                public void run()
                                {
                                    getUserInfo();
                                    System.out.println("User info obtained = " + currentUser);
                                    makeSleep(500);
                                    if (currentUser != null)
                                    {
                                        getStudentCursus(currentUser.get("id").getAsString());
                                        System.out.println("StudentCursus info obtained = " + userCursus);
                                        makeSleep(500);
                                        if (userCursus != null)
                                        {
                                            getStudentProject(currentUser.get("id").getAsString());
                                            System.out.println("StudentProject info obtained = " + userProject);
                                            if (userProject != null)
                                                isFind = true;
                                            System.out.println("Print = " + isFind);
                                        }
                                    }
                                }
                            });
                            userService.shutdown();
                            if (!userService.awaitTermination(60, TimeUnit.SECONDS))
                                userService.shutdownNow();
                            setContentView(R.layout.logged);
                            if (isFind)
                                showUserProfile(currentUser);
                        }
                        else
                            setContentView(R.layout.activity_main);
                    }
                    catch (InterruptedException ex)
                    {
                        tokenService.shutdownNow();
                        userService.shutdownNow();
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
        webView.loadUrl(Url);
    }

    private void showUserProfile(JsonObject user)
    {
        TextView                userLogin;
        TextView                userLvl;
        TextView                userEmail;
        TextView                userCorrectionpoint;
        TextView                userWallet;
        ListView                list;
        SwitchCompat            onOffSwitch;
        ArrayList<DataModel>    projectList;

        System.out.println("Printing user !");
        projectList = createProjectList(userProject);
        userLvl = findViewById(R.id.userLvl);
        userLogin = findViewById(R.id.userLogin);
        userEmail = findViewById(R.id.userEmail);
        userWallet = findViewById(R.id.userWallet);
        list = findViewById(R.id.projectList);

        System.out.println("View finded !");

        showImage(R.id.profilePic, getImageUrl(user));
        userCorrectionpoint = findViewById(R.id.userCorrectionPoint);
        userLogin.setText(user.get("login").getAsString());
        if (userCursus != null)
            userLvl.setText(userCursus.get("level").getAsString());
        else
            userLvl.setText("0");
        userEmail.setText(user.get("email").getAsString());
        userCorrectionpoint.setText(user.get("correction_point").getAsString());
        userWallet.setText(user.get("wallet").getAsString());

        System.out.println("Information printed for !" + user.get("login").getAsString());

        ListAdapter adapter = new ListAdapter(getApplicationContext(), projectList);
        list.setAdapter(adapter);

        onOffSwitch = findViewById(R.id.projectTitle);
        onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                ArrayList<DataModel>   skillList = new ArrayList<>();
                ListAdapter             adapter;

                if (isChecked)
                {
                    if (userCursus != null)
                        skillList = createSkillList(userCursus.get("skills").getAsJsonArray());
                    adapter = new ListAdapter(getApplicationContext(), skillList);
                }
                else
                {
                    adapter = new ListAdapter(getApplicationContext(), projectList);
                }
                list.setAdapter(adapter);
            }
        });
        isFind = false;
    }


    //===========       HTTP         ==========

    private void getStudentProject(final String id)
    {
        URL                     authUrl;
        HttpURLConnection       authCo = null;
        BufferedReader          in;
        Context                 context = this;

        try
        {
            String tmp = context.getResources().getString(R.string.user_data) + id + "/projects_users";
            authUrl = new URL(context.getResources().getString(R.string.user_data) + id + "/projects_users");
            authCo = (HttpsURLConnection) authUrl.openConnection();
            authCo.setRequestMethod("GET");
            authCo.setRequestProperty("authorization", "Bearer " + finalToken);
            if (authCo.getResponseCode() == HttpURLConnection.HTTP_OK)
            {
                String inputLine;

                in = new BufferedReader(new InputStreamReader(authCo.getInputStream()));
                StringBuffer res = new StringBuffer();
                while ((inputLine = in.readLine()) != null)
                {
                    res = res.append(inputLine);
                }
                userProject = JsonParser.parseString(res.toString()).getAsJsonArray();
                count = 0;
            }
            else
            {
                if (authCo.getResponseCode() == 401 && count < 5)
                {
                    count++;
                    getRefreshedToken();
                    getStudentProject(id);
                }
                System.out.println("code = " + authCo.getResponseCode());
                System.out.println("response = " + authCo.getResponseMessage());
            }
        }
        catch (Exception error)
        {
            userProject = null;
            System.out.print("error6 = ");
            error.printStackTrace();
        }
        finally
        {
            if (authCo != null)
                authCo.disconnect();
        }
    }

    private void getStudentCursus(final String id)
    {
        URL                 authUrl;
        HttpURLConnection   authCo = null;
        BufferedReader      in;
        Context             context = this;

        try
        {
            authUrl = new URL(context.getResources().getString(R.string.user_data) + id + "/cursus_users?filter[cursus_id]=21");
            authCo = (HttpsURLConnection) authUrl.openConnection();
            authCo.setRequestMethod("GET");
            authCo.setRequestProperty("authorization", "Bearer " + finalToken);
            if (authCo.getResponseCode() == HttpURLConnection.HTTP_OK)
            {
                String inputLine;

                in = new BufferedReader(new InputStreamReader(authCo.getInputStream()));
                StringBuffer res = new StringBuffer();
                while ((inputLine = in.readLine()) != null)
                {
                    res = res.append(inputLine);
                }
                JsonArray tmpArray = JsonParser.parseString(res.toString()).getAsJsonArray();
                if (tmpArray != null && tmpArray.size() != 0)
                    userCursus = tmpArray.get(0).getAsJsonObject();
                else
                    userCursus = null;
                count = 0;
            }
            else
            {
                if (authCo.getResponseCode() == 401 && count < 5)
                {
                    count++;
                    getRefreshedToken();
                    getStudentCursus(id);
                }
                System.out.println("code = " + authCo.getResponseCode());
                System.out.println("response = " + authCo.getResponseMessage());
            }
        }
        catch (Exception error)
        {
            userCursus = null;
            System.out.print("error5 = ");
            error.printStackTrace();
        }
        finally
        {
            if (authCo != null)
                authCo.disconnect();
        }
    }
    private void getStudentInfo(final String toSearch)
    {
        URL                 authUrl;
        HttpURLConnection   authCo = null;
        BufferedReader      in;
        Context             context = this;

        try
        {
            authUrl = new URL(context.getResources().getString(R.string.find_user) + toSearch);
            authCo = (HttpsURLConnection) authUrl.openConnection();
            authCo.setRequestMethod("GET");
            authCo.setRequestProperty("authorization", "Bearer " + finalToken);
            if (authCo.getResponseCode() == HttpURLConnection.HTTP_OK)
            {
                String inputLine;

                in = new BufferedReader(new InputStreamReader(authCo.getInputStream()));
                StringBuffer res = new StringBuffer();
                while ((inputLine = in.readLine()) != null)
                {
                    res = res.append(inputLine);
                }
                JsonArray tmpArray = JsonParser.parseString(res.toString()).getAsJsonArray();
                if (tmpArray != null && tmpArray.size() != 0)
                    findUser = tmpArray.get(0).getAsJsonObject();
                else
                    findUser = null;
                count = 0;
            }
            else
            {
                if (authCo.getResponseCode() == 401 && count < 5)
                {
                    count++;
                    getRefreshedToken();
                    getStudentInfo(toSearch);
                }
                System.out.println("code = " + authCo.getResponseCode());
                System.out.println("response = " + authCo.getResponseMessage());
            }
        }
        catch (Exception error)
        {
            findUser = null;
            System.out.print("error4 = ");
            System.out.println(error);
            error.printStackTrace();
        }
        finally
        {
            if (authCo != null)
                authCo.disconnect();
        }
    }

    private void getUserInfo()
    {
        URL                 authUrl;
        HttpURLConnection   authCo = null;
        BufferedReader      in;
        Context             context = this;

        try
        {
            authUrl = new URL(context.getResources().getString(R.string.me));
            authCo = (HttpsURLConnection) authUrl.openConnection();
            authCo.setRequestMethod("GET");
            authCo.setRequestProperty("authorization", "Bearer " + finalToken);
            if (authCo.getResponseCode() == HttpURLConnection.HTTP_OK)
            {
                String inputLine;

                in = new BufferedReader(new InputStreamReader(authCo.getInputStream()));
                StringBuffer res = new StringBuffer();
                while ((inputLine = in.readLine()) != null)
                {
                    res = res.append(inputLine);
                }
                currentUser = JsonParser.parseString(res.toString()).getAsJsonObject();
                count = 0;
            }
            else
            {
                if (authCo.getResponseCode() == 401 && count < 5)
                {
                    count++;
                    getRefreshedToken();
                    getUserInfo();
                }
                System.out.println("code = " + authCo.getResponseCode());
                System.out.println("response = " + authCo.getResponseMessage());
            }
        }
        catch (Exception error)
        {
            currentUser = null;
            System.out.print("error3 = ");
            error.printStackTrace();
        }
        finally
        {
            if (authCo != null)
                authCo.disconnect();
        }
    }

    private void getFinalToken()
    {
        Context             context = this;
        final String        post_Params = createFinalParam(temporaryToken, returnedState);
        URL                 authUrl;
        HttpURLConnection   authCo = null;
        BufferedReader      in;

        try
        {
            authUrl = new URL(context.getResources().getString(R.string.token_url));
            authCo = (HttpsURLConnection) authUrl.openConnection();
            authCo.setRequestMethod("POST");
            authCo.setDoOutput(true);
            OutputStream os = authCo.getOutputStream();
            os.write(post_Params.getBytes());
            os.flush();
            os.close();
            if (authCo.getResponseCode() == HttpURLConnection.HTTP_OK)
            {
                String inputLine;

                in = new BufferedReader(new InputStreamReader(authCo.getInputStream()));
                StringBuilder res = new StringBuilder();
                while ((inputLine = in.readLine()) != null)
                {
                    res.append(inputLine);
                }

                //b1956555a5f10346f0d77bafc89cd87c0add0c24301ce4f8784397a1915afb66

                System.out.println("Final = " + res);
                String[] tmp = res.toString().split(",");
                String[] finalTmp = tmp[0].split(":");
                finalToken = finalTmp[1].replaceAll("\"", "");
                String[] refreshTmp = tmp[3].split(":");
                refreshToken = refreshTmp[1].replaceAll("\"", "");
                //getRefreshedToken(); //delete
            }
            else
            {
                System.out.println("code = " + authCo.getResponseCode());
                System.out.println("response = " + authCo.getResponseMessage());
            }
        }
        catch (Exception error)
        {
            System.out.print("error2 = ");
            error.printStackTrace();
        }
        finally
        {
            if (authCo != null)
                authCo.disconnect();
        }
    }

    private void getRefreshedToken()
    {
        Context             context = this;
        final String        post_Params = createRefreshParam();
        URL                 authUrl;
        HttpURLConnection   authCo = null;
        BufferedReader      in;

        System.out.println("TRY TO OBTAIN REFRESHED TOKEN");
        try
        {
            authUrl = new URL(context.getResources().getString(R.string.token_url));
            authCo = (HttpsURLConnection) authUrl.openConnection();
            authCo.setRequestMethod("POST");
            authCo.setDoOutput(true);
            OutputStream os = authCo.getOutputStream();
            os.write(post_Params.getBytes());
            os.flush();
            os.close();
            if (authCo.getResponseCode() == HttpURLConnection.HTTP_OK)
            {
                String inputLine;

                in = new BufferedReader(new InputStreamReader(authCo.getInputStream()));
                StringBuilder res = new StringBuilder();
                while ((inputLine = in.readLine()) != null)
                {
                    res.append(inputLine);
                }
                System.out.println("Refresheeeeed = " + res);
                String[] tmp = res.toString().split(",");
                String[] finalTmp = tmp[0].split(":");
                finalToken = finalTmp[1].replaceAll("\"", "");
                String[] refreshTmp = tmp[3].split(":");
                refreshToken = refreshTmp[1].replaceAll("\"", "");
            }
            else
            {
                System.out.println("code = " + authCo.getResponseCode());
                System.out.println("response = " + authCo.getResponseMessage());
            }
        }
        catch (Exception error)
        {
            System.out.print("error7 = ");
            error.printStackTrace();
        }
        finally
        {
            if (authCo != null)
                authCo.disconnect();
        }
    }

    private final String createFinalParam(final String tmpToken, final String retState)
    {
        String      post_Params = "";
        Context     context = this;

        post_Params = post_Params.concat("grant_type=authorization_code");
        post_Params = post_Params.concat("&");
        post_Params = post_Params.concat("client_id=");
        post_Params = post_Params.concat(BuildConfig.API_ID);
        post_Params = post_Params.concat("&");
        post_Params = post_Params.concat("client_secret=");
        post_Params = post_Params.concat(BuildConfig.API_PRIVATE);
        post_Params = post_Params.concat("&");
        post_Params = post_Params.concat("code=");
        post_Params = post_Params.concat(tmpToken);
        post_Params = post_Params.concat("&");
        post_Params = post_Params.concat("redirect_uri=");
        post_Params = post_Params.concat(context.getResources().getString(R.string.uri));
        post_Params = post_Params.concat("&");
        post_Params = post_Params.concat("state=");
        post_Params = post_Params.concat(retState);
        return (post_Params);
    }

    private final String createRefreshParam()
    {
        String      post_Params = "";
        Context     context = this;

        post_Params = post_Params.concat("grant_type=refresh_token");
        post_Params = post_Params.concat("&");
        post_Params = post_Params.concat("client_id=");
        post_Params = post_Params.concat(BuildConfig.API_ID);
        post_Params = post_Params.concat("&");
        post_Params = post_Params.concat("client_secret=");
        post_Params = post_Params.concat(BuildConfig.API_PRIVATE);
        post_Params = post_Params.concat("&");
        post_Params = post_Params.concat("refresh_token=");
        post_Params = post_Params.concat(refreshToken);
        System.out.println("param = " + post_Params);
        return (post_Params);
    }


    //===========       UTILITY         ==========

    private ArrayList<DataModel> createSkillList(JsonArray skills)
    {
        ArrayList<DataModel>    result = new ArrayList<>();
        ExecutorService         skillService = Executors.newFixedThreadPool(5);

        skillService.execute(new Runnable()
        {
            public void run()
            {
                int                     i;
                int                     size;
                JsonObject              current;

                size = skills.size();
                i = 0;
                while (i < size)
                {
                    current = skills.get(i).getAsJsonObject();
                    if (current != null)
                        result.add(new DataModel(current.get("name").getAsString(), current.get("level").getAsString()));
                    i++;
                }
            }
        });
        skillService.shutdown();
        try
        {
            if (!skillService.awaitTermination(60, TimeUnit.SECONDS))
                skillService.shutdownNow();
        }
        catch (InterruptedException ex)
        {
            skillService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        return (result);

    }
    private ArrayList<DataModel> createProjectList(JsonArray project)
    {
        ArrayList<DataModel>    result = new ArrayList<>();
        ExecutorService         listService = Executors.newFixedThreadPool(5);

        listService.execute(new Runnable()
        {
            public void run()
            {
                int                     i;
                int                     size;
                JsonObject              current;
                JsonObject              tmp;
                JsonArray               cursus_list = new JsonArray();
                JsonElement             needed_cursus;


                cursus_list.add(21);
                needed_cursus = cursus_list.get(0);
                size = project.size();
                i = 0;
                while (i < size)
                {
                    current = project.get(i).getAsJsonObject();
                    if (current != null && current.get("cursus_ids").getAsJsonArray().size() != 0
                            && !current.get("cursus_ids").isJsonNull() && !current.get("final_mark").isJsonNull())
                    {
                        if (current.get("cursus_ids").getAsJsonArray().contains(needed_cursus))
                        {
                            tmp = current.get("project").getAsJsonObject();
                            result.add(new DataModel(tmp.get("name").getAsString(), current.get("final_mark").getAsString()));
                        }
                    }
                    i++;
                }
            }
        });
        listService.shutdown();
        try
        {
            if (!listService.awaitTermination(60, TimeUnit.SECONDS))
                listService.shutdownNow();
        }
        catch (InterruptedException ex)
        {
            listService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        return (result);
    }

    private void    showImage(final int targetId, final String url)
    {
        ImageView   target;

        target = findViewById(targetId);
        if (target != null)
            Picasso.get().load(url).fit().centerInside().into(target);
    }

    private String getImageUrl(final JsonObject user)
    {
        JsonObject  tmpImage;
        JsonObject  tmpVersion;
        String      url = null;

        tmpImage = user.get("image").getAsJsonObject();
        tmpVersion = tmpImage.get("versions").getAsJsonObject();
        if (!tmpVersion.get("small").isJsonNull())
            url = tmpVersion.get("small").getAsString();
        return (url);
    }

    private void makeSleep(int time)
    {
        try
        {
            Thread.sleep(time);
        }
        catch (Exception error)
        {
            System.out.print("error1 = ");
            error.printStackTrace();
        }
    }
    private String randomString(final int len)
    {
        String          seed = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder   salt = new StringBuilder();
        Random          rnd = new Random();

        while (salt.length() < len)
        {
            int index = (int) (rnd.nextFloat() * seed.length());
            salt.append(seed.charAt(index));
        }
        return (salt.toString());
    }
}