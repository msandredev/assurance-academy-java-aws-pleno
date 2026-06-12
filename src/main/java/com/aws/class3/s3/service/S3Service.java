package com.aws.class3.s3.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class S3Service {

    private final S3Client s3Client;

    @org.springframework.beans.factory.annotation.Value("${aws.s3.bucket}")
    private String BUCKET;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    // Upload de arquivo. prefix opcional, ex: "Aula-Java/"
    public void upload(MultipartFile file, String prefix) throws IOException {

        String key = (prefix == null || prefix.isBlank())
                ? file.getOriginalFilename()
                : normalizePrefix(prefix) + file.getOriginalFilename();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(
                request,
                software.amazon.awssdk.core.sync.RequestBody
                        .fromBytes(file.getBytes())
        );
    }

    // Retorna a árvore completa de arquivos e pastas a partir de um prefixo
    public TreeNode tree(String prefix) {
        String normalized = (prefix == null || prefix.isBlank())
                ? ""
                : normalizePrefix(prefix);
        return buildTree(normalized);
    }

    // Lista arquivos e pastas num dado nível (plano)
    public ListResult list(String prefix) {
        String normalized = (prefix == null || prefix.isBlank())
                ? ""
                : normalizePrefix(prefix);

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(BUCKET)
                .prefix(normalized)
                .delimiter("/")
                .build();

        ListObjectsV2Response response = s3Client.listObjectsV2(request);

        List<FolderItem> folders = response.commonPrefixes()
                .stream()
                .map(p -> new FolderItem(p.prefix()))
                .toList();

        List<FileItem> files = new ArrayList<>();
        for (S3Object obj : response.contents()) {
            if (!obj.key().equals(normalized)) {
                files.add(FileItem.fromS3Object(obj));
            }
        }

        return new ListResult(folders, files);
    }

    private TreeNode buildTree(String prefixPath) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(BUCKET)
                .prefix(prefixPath)
                .delimiter("/")
                .build();

        ListObjectsV2Response response = s3Client.listObjectsV2(request);

        List<TreeNode> children = new ArrayList<>();

        for (CommonPrefix cp : response.commonPrefixes()) {
            children.add(buildTree(cp.prefix()));
        }

        for (S3Object obj : response.contents()) {
            if (!obj.key().equals(prefixPath)) {
                String name = obj.key().contains("/")
                        ? obj.key().substring(obj.key().lastIndexOf('/') + 1)
                        : obj.key();
                children.add(new TreeNode(
                        name, obj.key(), "file", null,
                        obj.size(), obj.lastModified(), obj.eTag()
                ));
            }
        }

        String displayPath = prefixPath.isEmpty() ? "/" : prefixPath;
        String name = "/";
        if (!displayPath.equals("/")) {
            String trimmed = displayPath.substring(0, displayPath.length() - 1);
            name = trimmed.contains("/")
                    ? trimmed.substring(trimmed.lastIndexOf('/') + 1)
                    : trimmed;
        }

        return new TreeNode(name, prefixPath, "folder", children, null, null, null);
    }

    // Obtém os bytes do arquivo
    public ResponseInputStream<GetObjectResponse> getFile(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .build();

        return s3Client.getObject(request);
    }

    // Obtém metadados do arquivo (tamanho, content-type, etc.) sem baixar
    public HeadObjectResponse getMetadata(String key) {
        HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .build();

        return s3Client.headObject(request);
    }

    private String normalizePrefix(String prefix) {
        return prefix.endsWith("/") ? prefix : prefix + "/";
    }

    public record TreeNode(
            String name,
            String path,
            String type,
            List<TreeNode> children,
            Long size,
            Instant lastModified,
            String eTag
    ) {}

    public record ListResult(List<FolderItem> folders, List<FileItem> files) {}

    public record FolderItem(String prefix) {}

    public record FileItem(String key, Long size, Instant lastModified, String eTag) {
        static FileItem fromS3Object(S3Object obj) {
            return new FileItem(obj.key(), obj.size(), obj.lastModified(), obj.eTag());
        }
    }
}