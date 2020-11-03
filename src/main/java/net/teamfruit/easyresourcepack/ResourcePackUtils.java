package net.teamfruit.easyresourcepack;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.Closeable;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class ResourcePackUtils {
    public static CompletableFuture<Optional<String>> downloadAndGetHash(String urlString) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get Request
                URL url = new URL(urlString);
                HttpURLConnection http = (HttpURLConnection) url.openConnection();
                try (Closeable closeable = http::disconnect) {
                    http.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36");
                    http.setRequestMethod("GET");
                    http.connect();

                    if (http.getResponseCode() == HttpURLConnection.HTTP_OK)
                        try (InputStream in = http.getInputStream()) {
                            return Optional.of(createSha1(in));
                        }
                }
            } catch (Exception e) {
                EasyResourcePack.logger.log(Level.WARNING, "Failed to calculate hash", e);
            }
            return Optional.empty();
        });
    }

    public static String createSha1(InputStream input) throws Exception  {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");

        byte[] buffer = new byte[8192];
        int len = input.read(buffer);

        while (len != -1) {
            sha1.update(buffer, 0, len);
            len = input.read(buffer);
        }

        return new HexBinaryAdapter().marshal(sha1.digest());
    }
}
