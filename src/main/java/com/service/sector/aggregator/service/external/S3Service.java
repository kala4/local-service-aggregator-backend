package com.service.sector.aggregator.service.external;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;

@Service
public class S3Service {

    private final S3Client s3;
    private final String bucketName;
    private final String publicUrlPrefix;

    public S3Service(
            @Value("${aws.s3.bucket:workplace-photos}") String bucketName,
            @Value("${aws.s3.region:eu-north-1}") String region,
            @Value("${aws.s3.endpoint:#{null}}") String endpoint,
            @Value("${aws.s3.access-key:#{null}}") String accessKey,
            @Value("${aws.s3.secret-key:#{null}}") String secretKey,
            @Value("${aws.s3.public-url-prefix:#{null}}") String publicUrlPrefix,
            @Value("${aws.s3.path-style-enabled:false}") boolean pathStyleEnabled
    ) {
        this.bucketName = bucketName;

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region));

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        if (accessKey != null && secretKey != null && !accessKey.isBlank() && !secretKey.isBlank()) {
            builder.credentialsProvider(
                    StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
            );
        }

        if (pathStyleEnabled) {
            builder.serviceConfiguration(b -> b.pathStyleAccessEnabled(true));
        }

        this.s3 = builder.build();

        if (publicUrlPrefix != null && !publicUrlPrefix.isBlank()) {
            this.publicUrlPrefix = publicUrlPrefix.endsWith("/") ? publicUrlPrefix : publicUrlPrefix + "/";
        } else if (endpoint != null && !endpoint.isBlank()) {
            this.publicUrlPrefix = endpoint.endsWith("/") ? endpoint + bucketName + "/" : endpoint + "/" + bucketName + "/";
        } else {
            this.publicUrlPrefix = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);
        }
    }

    public String upload(byte[] bytes, String key, String contentType) {
        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        s3.putObject(req, RequestBody.fromBytes(bytes));
        return this.publicUrlPrefix + key;
    }
}