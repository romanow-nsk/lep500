package me.romanow.lep500;

import com.yandex.disk.rest.Credentials;
import com.yandex.disk.rest.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class YandexDiskService {
    private final MainActivity face;
    private RestClient client;
    public YandexDiskService(MainActivity face) {
        this.face = face;
        //Credentials credentials = new Credentials();
        //client = new RestClient(credentials);
        }
    public void init(){

        }
    public static String generateSHA256(String src) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(src.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder(3 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            if (i!=0)
                hexString.append(":");
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
                }
            hexString.append(hex);
            }
        return hexString.toString();
        }
    public static void main(String aa[]) throws NoSuchAlgorithmException {
        System.out.println(generateSHA256("Romanow Solus Rex"));
        }

}
