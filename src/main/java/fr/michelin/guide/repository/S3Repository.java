package fr.michelin.guide.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;

// Accès au stockage S3 : génère des liens présignés pour envoyer ou afficher un fichier
@Repository
public class S3Repository {

    // Durée de validité des liens générés
    private static final Duration DUREE_LIEN = Duration.ofMinutes(10);

    private final S3Presigner presigner;

    // Les identifiants du S3 sont lus dans application.yml
    public S3Repository(@Value("${s3.url}") String url,
                        @Value("${s3.accessKeyId}") String accessKeyId,
                        @Value("${s3.secretAccessKey}") String secretAccessKey) {
        this.presigner = S3Presigner.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.builder()
                        .accessKeyId(accessKeyId)
                        .secretAccessKey(secretAccessKey)
                        .build()))
                .endpointOverride(URI.create(url))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    // Lien pour envoyer (PUT) un fichier dans le bucket
    public String getUploadUrl(String bucket, String key) {
        PutObjectPresignRequest request = PutObjectPresignRequest.builder()
                .signatureDuration(DUREE_LIEN)
                .putObjectRequest(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build())
                .build();
        return presigner.presignPutObject(request).url().toString();
    }

    // Lien pour afficher (GET) un fichier du bucket
    public String getDownloadUrl(String bucket, String key) {
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(DUREE_LIEN)
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build())
                .build();
        return presigner.presignGetObject(request).url().toString();
    }
}
