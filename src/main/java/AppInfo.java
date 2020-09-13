public interface AppInfo {
    String APP_ID = System.getProperty("CLIENT", "");
    String TENANT = System.getProperty("TENANT", "");//
    String AUTHORITY = "https://login.microsoftonline.com/" + TENANT + "/oauth2/token";
}
