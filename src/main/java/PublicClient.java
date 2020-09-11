// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

import com.microsoft.aad.msal4j.AuthenticationResult;
import com.microsoft.aad.msal4j.PublicClientApplication;
import com.microsoft.aad.msal4j.UserNamePasswordParameters;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.core.DefaultClientConfig;
import com.microsoft.graph.core.IClientConfig;
import com.microsoft.graph.http.IHttpRequest;
import com.microsoft.graph.models.extensions.Drive;
import com.microsoft.graph.models.extensions.DriveItem;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.requests.extensions.*;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class PublicClient {

    private final static String APP_ID = System.getProperty("CLIENT","");
    private static final String TENANT =System.getProperty("TENANT","");//

    private final static String AUTHORITY = "https://login.microsoftonline.com/" + TENANT + "/oauth2/token";


    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {

        String userName=System.getProperty("USER");

        String password=System.getProperty("PASSWORD");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                System.in))) {
            if (userName==null) {
                System.out.print("Enter username: ");
                userName = br.readLine();
                System.out.print("Enter password: ");
                password = br.readLine();
            }

            // Request access token from AAD
            AuthenticationResult result = getAccessToken(userName, password);

            // Get user info from Microsoft Graph
            String userInfo = getUserInfoFromGraph(result.accessToken());
            System.out.println(userInfo);
            System.out.println("Expires on: "+result.expiresOn());
            System.out.println("Expires on: "+result.expiresOnDate());
            IClientConfig config=new DefaultClientConfig() {
                @Override
                public IAuthenticationProvider getAuthenticationProvider() {
                    return mAuthenticationProvider;
                }

                IAuthenticationProvider mAuthenticationProvider = new IAuthenticationProvider() {
                    @Override
                    public void authenticateRequest(final IHttpRequest request) {
                        request.addHeader("Authorization",
                                "Bearer " + result.accessToken());
                    }
                };
            };
            IGraphServiceClient client=GraphServiceClient.fromConfig(config);

            IDriveRequest request = client.me()
                    .drive()
                    .buildRequest();


            Drive theDrive = request.get();
            System.out.println("Drive ID is: "+theDrive.id);

            IDriveItemCollectionRequest req = client.me().drive().root().children().buildRequest();


            IDriveItemCollectionPage collection = req.get();




            List<DriveItem> driveItems = collection.getCurrentPage();
            driveItems.forEach(driveItem -> {
                System.out.println("Name: "+driveItem.name);
                System.out.println("User: "+driveItem.createdBy.user.displayName);
            });







        }
    }

    private static AuthenticationResult getAccessToken(String userName, String password)
            throws MalformedURLException, InterruptedException, ExecutionException {

            PublicClientApplication pca = PublicClientApplication.builder(
                    APP_ID).
                    authority(AUTHORITY).build();

            String scope = "User.Read";
        Set<String> scopes = new HashSet<>();
        scopes.add(scope);scopes.add("Files.Read");
        UserNamePasswordParameters parameters = UserNamePasswordParameters.builder(
                scopes,
                    userName,
                    password.toCharArray()).build();

            AuthenticationResult result = pca.acquireToken(parameters).get();
            return result;
        }

    private static String getUserInfoFromGraph(String accessToken) throws IOException{
        URL url = new URL("https://graph.microsoft.com/v1.0/me");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Accept","application/json");

        int httpResponseCode = conn.getResponseCode();
        if(httpResponseCode == HTTPResponse.SC_OK) {

            StringBuilder response;
            try(BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))){

                String inputLine;
                response = new StringBuilder();
                while (( inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            }
            return response.toString();
        } else {
            return String.format("Connection returned HTTP code: %s with message: %s",
                    httpResponseCode, conn.getResponseMessage());
        }
    }
}
