package tn.esprit.services;

import tn.esprit.entities.Video;
import tn.esprit.utils.MyDatabase;
import tn.esprit.utils.CloudinaryConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.util.Map;

public class VideoService {

    private Connection cnx() {
        return MyDatabase.getInstance().getConnection();
    }

    // ================= ADD =================
    public boolean addVideo(Video v) {

        String sql = "INSERT INTO video(title, path, public_id, thumbnail) VALUES(?,?,?,?)";

        try (PreparedStatement ps = cnx().prepareStatement(sql)) {

            ps.setString(1, v.getTitle());
            ps.setString(2, v.getPath());
            ps.setString(3, v.getPublicId());
            ps.setString(4, v.getThumbnail());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error addVideo: " + e.getMessage());
        }

        return false;
    }

    // ================= GET ALL =================
    public List<Video> getAllVideos() {

        List<Video> list = new ArrayList<>();

        String sql = "SELECT * FROM video ORDER BY id DESC";

        try (Statement st = cnx().createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {

                list.add(new Video(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("path"),
                        rs.getString("public_id"),
                        rs.getString("thumbnail")
                ));
            }

        } catch (SQLException e) {
            System.err.println("Error getAllVideos: " + e.getMessage());
        }

        return list;
    }

    // ================= GET BY ID =================
    public Video getVideoById(int id) {

        String sql = "SELECT * FROM video WHERE id=?";

        try (PreparedStatement ps = cnx().prepareStatement(sql)) {

            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Video(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("path"),
                        rs.getString("public_id"),
                        rs.getString("thumbnail")
                );
            }

        } catch (SQLException e) {
            System.err.println("Error getVideoById: " + e.getMessage());
        }

        return null;
    }

    // ================= DELETE =================
    public boolean deleteVideo(int id) {

        Video v = getVideoById(id);

        if (v != null) {
            try {
                CloudinaryConfig.getInstance().uploader().destroy(
                        v.getPublicId(),
                        Map.of("resource_type", "video")
                );
            } catch (Exception e) {
                System.err.println("Cloud delete error: " + e.getMessage());
            }
        }

        String sql = "DELETE FROM video WHERE id=?";

        try (PreparedStatement ps = cnx().prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error deleteVideo: " + e.getMessage());
        }

        return false;
    }

    // ================= UPDATE =================
    public boolean updateVideo(Video v) {

        String sql = "UPDATE video SET title=?, path=?, public_id=?, thumbnail=? WHERE id=?";

        try (PreparedStatement ps = cnx().prepareStatement(sql)) {

            ps.setString(1, v.getTitle());
            ps.setString(2, v.getPath());
            ps.setString(3, v.getPublicId());
            ps.setString(4, v.getThumbnail());
            ps.setInt(5, v.getId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updateVideo: " + e.getMessage());
        }

        return false;
    }

    // ================= UPLOAD CLOUDINARY =================
    public Video uploadToCloudinary(File videoFile) {

        try {

            Map<?, ?> result = CloudinaryConfig.getInstance()
                    .uploader()
                    .upload(videoFile, Map.of(
                            "resource_type", "video",
                            "folder", "videos"
                    ));

            String videoUrl = result.get("secure_url").toString();
            String publicId = result.get("public_id").toString();

            // 🎯 thumbnail auto (frame à 2 sec)
            String thumbnailUrl =
                    "https://res.cloudinary.com/dsekpiknh/video/upload/so_2,w_400,h_250,c_fill/"
                            + publicId + ".jpg";

            return new Video(
                    videoFile.getName(),
                    videoUrl,
                    publicId,
                    thumbnailUrl
            );

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}