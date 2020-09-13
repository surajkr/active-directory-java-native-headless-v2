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


import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.PublicClientApplication;
import com.microsoft.aad.msal4j.UserNamePasswordParameters;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.concurrency.ChunkedUploadProvider;
import com.microsoft.graph.concurrency.IProgressCallback;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.core.DefaultClientConfig;
import com.microsoft.graph.core.IClientConfig;
import com.microsoft.graph.http.IHttpRequest;
import com.microsoft.graph.models.extensions.*;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import com.microsoft.graph.requests.extensions.IDriveItemCollectionPage;
import com.microsoft.graph.requests.extensions.IDriveItemCollectionRequest;
import com.microsoft.graph.requests.extensions.IDriveRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;

import java.io.File;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class PublicClient implements AppInfo
{


    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
        String uploadFilePath = null;
        if (args.length > 0) {
            uploadFilePath = args[0];
        }

        IAuthenticationResult result = initAuthentication();


        IGraphServiceClient client = initGraphServiceClient(result);

        IDriveRequest request = client.me()
                .drive()
                .buildRequest();


        Drive theDrive = request.get();
        System.out.println("Drive ID is: " + theDrive.id);

        IDriveItemCollectionRequest req = client.me().drive().root().children().buildRequest();


        IDriveItemCollectionPage collection = req.get();


        List<DriveItem> driveItems = collection.getCurrentPage();
        driveItems.forEach(driveItem -> {
            System.out.println("Name: " + driveItem.name);
            System.out.println("User: " + driveItem.createdBy.user.displayName);
        });


        createFolder(client, "name." + System.currentTimeMillis());
        if (uploadFilePath != null) {
            uploadFile(client, uploadFilePath);
        }

    }

    static IAuthenticationResult initAuthentication() throws IOException, InterruptedException, ExecutionException {
        String userName = System.getProperty("USER");

        String password = System.getProperty("PASSWORD");
        IAuthenticationResult result = null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                System.in))) {
            if (userName == null) {
                System.out.print("Enter username: ");
                userName = br.readLine();
                System.out.print("Enter password: ");
                password = br.readLine();
            }

            // Request access token from AAD
            result = getAccessToken(userName, password);
        }
        return result;
    }


    static IGraphServiceClient initGraphServiceClient(IAuthenticationResult result) throws IOException {
        // Get user info from Microsoft Graph
        String userInfo = getUserInfoFromGraph(result.accessToken());
        System.out.println(userInfo);

        System.out.println("Expires on: " + result.expiresOnDate());
        IClientConfig config = new DefaultClientConfig() {
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
        return GraphServiceClient.fromConfig(config);
    }

    private static void createFolder(IGraphServiceClient client, String folderName) {

        DriveItem driveItem = new DriveItem();
        driveItem.name = folderName;
        driveItem.folder = new Folder();
        client.me().drive().root().children().buildRequest().post(driveItem);

    }

    public static void uploadFile(IGraphServiceClient client, String sourceFile) throws IOException {
        File source = new File(sourceFile);
        String itemPath = "/NewDutFolder/" + source.getName();

        UploadSession uploadSession = client
                .me()
                .drive()
                .root()
                // itemPath like "/Folder/file.txt"
                // does not need to be a path to an existing item
                .itemWithPath(itemPath)
                .createUploadSession(new DriveItemUploadableProperties())
                .buildRequest()
                .post();

        ChunkedUploadProvider<DriveItem> chunkedUploadProvider =
                new ChunkedUploadProvider<DriveItem>
                        (uploadSession, client, new BufferedInputStream(new FileInputStream(source)),
                                source.length(), DriveItem.class);

// Config parameter is an array of integers
// customConfig[0] indicates the max slice size
// Max slice size must be a multiple of 320 KiB
        int[] customConfig = {320 * 1024};

// Do the upload
        final DriveItemIProgressCallback callback = new DriveItemIProgressCallback();
        chunkedUploadProvider.upload(callback, customConfig);
        synchronized (callback) {
            try {
                while (callback.uploaded == null) {
                    callback.wait(1000L);

                }
                System.out.println("PublicClient.uploadFile:Completed " + callback.uploaded.name);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }


    private static IAuthenticationResult getAccessToken(String userName, String password)
            throws MalformedURLException, InterruptedException, ExecutionException
    {

        PublicClientApplication.Builder builder = PublicClientApplication.builder(
                AppInfo.APP_ID).
                authority(AppInfo.AUTHORITY);
        TokenCache.TokenPersistence val=null;
        boolean useTokenCache=Boolean.getBoolean("TOKEN_CACHE");
        val = TokenCache.initCache(builder, "PublicClient.token.cache.json");
        PublicClientApplication pca = builder.build();

        String scope = "User.Read";
        Set<String> scopes = new HashSet<>();
        scopes.add(scope);
        scopes.add("Files.Read");
        UserNamePasswordParameters parameters = UserNamePasswordParameters.builder(
                scopes,
                userName,
                password.toCharArray()).build();

        IAuthenticationResult result = pca.acquireToken(parameters).get();
        if(useTokenCache)
        {
            TokenCache.writeResource(val.data,"PublicClient.token.cache.json");

        }
        return result;
    }

    private static String getUserInfoFromGraph(String accessToken) throws IOException {
        URL url = new URL("https://graph.microsoft.com/v1.0/me");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Accept", "application/json");

        int httpResponseCode = conn.getResponseCode();
        if (httpResponseCode == HTTPResponse.SC_OK) {

            StringBuilder response;
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {

                String inputLine;
                response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            }
            return response.toString();
        } else {
            return String.format("Connection returned HTTP code: %s with message: %s",
                    httpResponseCode, conn.getResponseMessage());
        }
    }


    private static class DriveItemIProgressCallback implements IProgressCallback<DriveItem> {
        private DriveItem uploaded;

        @Override
        public void progress(long l, long l1) {
            System.out.println("ProgressCallback.progress: " + l + " of :  " + l1);

        }

        @Override
        public void success(DriveItem driveItem) {
            synchronized (this) {
                this.uploaded = driveItem;
                this.notifyAll();
            }

        }

        @Override
        public void failure(ClientException e) {

        }
    }
}

