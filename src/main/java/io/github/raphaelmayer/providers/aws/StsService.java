package io.github.raphaelmayer.providers.aws;

import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

public class StsService {

    private final StsClient stsClient;

    public StsService(StsClient stsClient) {
        this.stsClient = stsClient;
    }

    /**
     * Retrieve the AWS account ID using STS
     */
    public String getAccountId() {
        GetCallerIdentityResponse identityResponse = this.stsClient
                .getCallerIdentity(GetCallerIdentityRequest.builder().build());
        return identityResponse.account();
    }
}
