import com.microsoft.aad.msal4j.ITokenCacheAccessAspect;
import com.microsoft.aad.msal4j.ITokenCacheAccessContext;
import com.microsoft.aad.msal4j.PublicClientApplication;

import java.io.*;

public class TokenCache
{
    public static TokenPersistence initCache(PublicClientApplication.Builder builder, String cachePath) {
        TokenPersistence valCache=null;
        String dataToInitCache = readResource(cachePath);
        valCache = new TokenPersistence(dataToInitCache);
        builder.setTokenCacheAccessAspect(valCache);
        return valCache;
    }

    private static String readResource(String cachePath)
    {
        String returnValue=null;
        try (BufferedReader reader = new BufferedReader(new FileReader(cachePath)))
        {
            StringBuilder cache=new StringBuilder("");
            reader.lines().forEach(cache::append);
            returnValue=cache.toString();


        }
        catch (IOException e) {

            e.printStackTrace();
        }
        return returnValue;
    }

    static void writeResource(String data, String cacheStore)
    {
        try(BufferedWriter writer=new BufferedWriter(new FileWriter(cacheStore)))
        {


        writer.write(data);

        }
         catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
    }

    public static class TokenPersistence implements ITokenCacheAccessAspect
    {
        String data;

        TokenPersistence(String data) {
            this.data = data;
        }

        @Override
        public void beforeCacheAccess(ITokenCacheAccessContext iTokenCacheAccessContext) {
            iTokenCacheAccessContext.tokenCache().deserialize(data);
        }

        @Override
        public void afterCacheAccess(ITokenCacheAccessContext iTokenCacheAccessContext) {
            data = iTokenCacheAccessContext.tokenCache().serialize();

        }
    }

}
