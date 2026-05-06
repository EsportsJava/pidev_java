package tn.esprit.utils;

import com.cloudinary.Cloudinary;
import java.util.HashMap;
import java.util.Map;

public class CloudinaryConfig {

    private static Cloudinary cloudinary;

    public static Cloudinary getInstance() {

        if (cloudinary == null) {

            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dsekpiknh");
            config.put("api_key", "746214727612689");
            config.put("api_secret", "RaOJbo5sVCH7Ydged5VqbLHHmBk");

            cloudinary = new Cloudinary(config);
        }

        return cloudinary;
    }
}