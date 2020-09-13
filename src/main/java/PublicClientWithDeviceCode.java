import com.microsoft.aad.msal4j.*;
import com.microsoft.aad.msal4j.DeviceCodeFlowParameters.DeviceCodeFlowParametersBuilder;
import com.microsoft.graph.models.extensions.Drive;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.requests.extensions.IDriveRequest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class PublicClientWithDeviceCode implements AppInfo
{
    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {

        String uploadFilePath = null;
        if (args.length > 0) {
            uploadFilePath = args[0];
        }

        IAuthenticationResult result = getAccessToken();


        IGraphServiceClient client = PublicClient.initGraphServiceClient(result);

        IDriveRequest request = client.me()
                .drive()
                .buildRequest();


        Drive theDrive = request.get();
        System.out.println("Drive ID is: " + theDrive.id);


    }


    private static IAuthenticationResult getAccessToken()
            throws MalformedURLException, InterruptedException, ExecutionException
    {

        boolean useTokenCache=Boolean.getBoolean("TOKEN_CACHE");

        PublicClientApplication.Builder builder1 = PublicClientApplication.builder(
                APP_ID).
                authority(AUTHORITY);
        TokenCache.TokenPersistence val=null;
        String cacheStore = "deviceCode.token.cache.json";
        if(useTokenCache)
        {
            val= TokenCache.initCache(builder1, cacheStore);

        }
        PublicClientApplication pca = builder1.build();

        String scope = "User.Read";
        Set<String> scopes = new HashSet<>();
        scopes.add(scope);
        scopes.add("Files.Read");
        Consumer<DeviceCode> deviceCodeConsumer = (DeviceCode deviceCode) -> System.out.println(deviceCode.message());
        DeviceCodeFlowParametersBuilder builder = DeviceCodeFlowParameters.builder(scopes, deviceCodeConsumer);


        IAuthenticationResult result = pca.acquireToken(builder.build()).get();
        if(useTokenCache)
        {
            TokenCache.writeResource(val.data, cacheStore);
        }

        return result;
    }


}
