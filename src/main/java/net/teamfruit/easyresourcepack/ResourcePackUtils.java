package net.teamfruit.easyresourcepack;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ResourcePackUtils {
    public static CompletableFuture<PackResult> downloadAndGetHash(String urlString) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get Request
                URL url = new URL(urlString);
                HttpURLConnection http = (HttpURLConnection) url.openConnection();
                try (Closeable closeable = http::disconnect) {
                    http.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36");
                    http.setRequestMethod("GET");
                    http.connect();

                    if (http.getResponseCode() != HttpURLConnection.HTTP_OK)
                        throw new RuntimeException("Invalid HTTP status code: " + http.getResponseCode());

                    try (InputStream in = http.getInputStream()) {
                        byte[] bytes = ByteStreams.toByteArray(ByteStreams.limit(in, 104857600));

                        if (in.available() > 0)
                            throw new RuntimeException("Size is larger than 104857600 bytes");

                        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(bytes))) {
                            ZipEntry entry;
                            while ((entry = zin.getNextEntry()) != null) {
                                if (entry.getName().equals("pack.mcmeta")) {
                                    try (InputStreamReader rd = new InputStreamReader(zin)) {
                                        PackMcMeta meta = new Gson().fromJson(rd, PackMcMeta.class);

                                        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
                                        sha1.update(bytes);
                                        String hash = new HexBinaryAdapter().marshal(sha1.digest());

                                        return new PackResult(true, meta, hash, null);
                                    }
                                }
                            }
                            throw new RuntimeException("pack.mcmeta is not found");
                        }
                    }
                }
            } catch (Exception e) {
                EasyResourcePack.logger.log(Level.WARNING, "Failed to calculate hash", e);
                return new PackResult(false, null, null, e.getMessage());
            }
        });
    }

    public static class PackResult {
        public boolean success;
        public PackMcMeta meta;
        public String hash;
        public String reason;

        public PackResult(boolean success, PackMcMeta meta, String hash, String reason) {
            this.success = success;
            this.meta = meta;
            this.hash = hash;
            this.reason = reason;
        }
    }

    public static class PackMcMeta {
        public PackMcMetaPack pack;

        public static class PackMcMetaPack {
            public int pack_format;
            public String description;
        }
    }
}
