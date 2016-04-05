/**
 * Created by AlejandroSA on 15/03/2016.
 */
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import com.google.api.services.drive.Drive;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.*;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;
import gmailsettings.GmailSettingsService;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Controller {
    /** Application name. */
    private static final String APPLICATION_NAME =
            "AutomaticSignatures";

    /** Directory to store user credentials for this application. */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(
            System.getProperty("user.home"), ".credentials/google-credentials.json");

    /** Global instance of the {@link FileDataStoreFactory}. */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;

    /** Global instance of admin email so we can use it to compare with the email.*/
    private static final String ADMIN_EMAIL = "asoriano@as.cloudimpulsion.com";

    /** Global instance of the scopes required by this quickstart.
     *
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials/drive-java-quickstart.json
     */
    private static final List<String> SCOPES =
            Arrays.asList(
                    "https://apps-apis.google.com/a/feeds/emailsettings/2.0/",
                    "https://spreadsheets.google.com/feeds");

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static String domain_name = "as.cloudimpulsion.com";

    /**
     * Creates an authorized Credential object.
     * @return an authorized Credential object.
     * @throws IOException
     */
    public static Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in =
                new FileInputStream("secret.json");
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                        .setDataStoreFactory(DATA_STORE_FACTORY)
                        .setAccessType("online")
                        .build();
        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()).authorize("user");
        System.out.println(
                "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }

    /**
     * Build and return an authorized Drive client service.
     * @return an authorized Drive client service
     * @throws IOException
     */

    public static void main(String[] args) throws IOException, ServiceException {
        // Build a new authorized API client service.

        getSpreadSheetInfo(authorize());

        /*try {
            updateSignature(authorize());
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    private static void getSpreadSheetInfo(Credential credential) throws IOException, ServiceException
    {
        SpreadsheetService service =
                new SpreadsheetService("MySpreadsheetIntegration-v1");

        // We give the credentials to work using Oauth2.0
        service.setOAuth2Credentials(credential);

        // Define the URL to request.  This should never change.
        URL SPREADSHEET_FEED_URL = new URL(
                "https://spreadsheets.google.com/feeds/spreadsheets/private/full");

        // Make a request to the API and get all spreadsheets.
        SpreadsheetFeed feed = service.getFeed(SPREADSHEET_FEED_URL,
                SpreadsheetFeed.class);
        List<SpreadsheetEntry> spreadsheets = feed.getEntries();

        if (spreadsheets.size() == 0) {
            // TODO: There were no spreadsheets, act accordingly.
        }

        // Search for the needed spreadsheet where we store the signature settings
        SpreadsheetEntry spreadsheet = null;

        for (SpreadsheetEntry entry: spreadsheets) {
            if (entry.getTitle().getPlainText().equals("TestingSignatures"))
            {
                spreadsheet = entry;
            }
        }
        if (spreadsheet != null)
        {
            System.out.println(spreadsheet.getTitle().getPlainText());

            WorksheetFeed worksheetFeed = service.getFeed(
                    spreadsheet.getWorksheetFeedUrl(), WorksheetFeed.class);
            List<WorksheetEntry> worksheets = worksheetFeed.getEntries();
            WorksheetEntry worksheet = worksheets.get(0);

            // Fetch the list feed of the worksheet.
            URL listFeedUrl = worksheet.getListFeedUrl();
            System.out.println(listFeedUrl);
            ListFeed listFeed = service.getFeed(listFeedUrl, ListFeed.class);


            // Iterate through each row, printing its cell values.
            //WARNING, It just reads from the second row until the first blank row

            int columna = 1;
            for (ListEntry row : listFeed.getEntries()) {
                // Print the first column's cell value
                System.out.print(row.getTitle().getPlainText() + "\t");
                // Iterate over the remaining columns, and print each cell value
                for (String tag : row.getCustomElements().getTags()) {
                    System.out.print("Columna "+columna+". "+row.getCustomElements().getValue(tag) + "\t");
                    columna++;
                }
                columna = 1;
            }
        }
        else
        {
            //TODO: The specified spreadsheet is not found at the list.
        }
    }

    private static void updateSignature(Credential credential) throws Exception {

        GmailSettingsService service = new GmailSettingsService(APPLICATION_NAME, domain_name, null, null){
            @Override
            public void setUserCredentials(String username, String password)
                    throws AuthenticationException {
                // Nothing to do here, just Overriding the old method and setting it to null so we can later setOauthCredentials to the service
            }};

        service.setOAuth2Credentials(credential);
        List users=new ArrayList();
        users.add("abell");
        service.changeSignature(users,
                "This is abell signature by Alejandro");
    }
}