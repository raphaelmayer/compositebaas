package io.github.raphaelmayer.providers.aws;

import java.util.List;
import java.util.stream.Collectors;

import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.AttachRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.CreateRoleRequest;
import software.amazon.awssdk.services.iam.model.CreateRoleResponse;
import software.amazon.awssdk.services.iam.model.DeleteRoleRequest;
import software.amazon.awssdk.services.iam.model.DetachRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.ListRolesResponse;
import software.amazon.awssdk.services.iam.model.Role;

public class IamService {

    private final IamClient iamClient;
    private final List<String> lambdaPolicies;

    public IamService(IamClient iamClient, List<String> lambdaPolicies) {
        this.iamClient = iamClient;
        this.lambdaPolicies = lambdaPolicies;
    }

    public String createAWSRole(String name) {
        String trustPolicy = "{\"Version\": \"2012-10-17\", \"Statement\": [{\"Effect\": \"Allow\", \"Principal\": {\"Service\": \"lambda.amazonaws.com\"}, \"Action\": \"sts:AssumeRole\"}]}";
        CreateRoleRequest request = CreateRoleRequest.builder()
                .roleName(name)
                .assumeRolePolicyDocument(trustPolicy)
                .build();
        CreateRoleResponse response = iamClient.createRole(request);

        lambdaPolicies.forEach(policyArn -> attachPolicyToRole(name, policyArn));
        Role role = response.role();
        try {
            Thread.sleep(7000); // Sleep to handle eventual consistency
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return role.arn();
    }

    public void deleteAWSRole(String name) {
        lambdaPolicies.forEach(policyArn -> detachPolicyFromRole(name, policyArn));
        DeleteRoleRequest request = DeleteRoleRequest.builder()
                .roleName(name)
                .build();
        iamClient.deleteRole(request);
    }

    public void attachPolicyToRole(String roleName, String policyArn) {
        AttachRolePolicyRequest request = AttachRolePolicyRequest.builder()
                .roleName(roleName)
                .policyArn(policyArn)
                .build();
        iamClient.attachRolePolicy(request);
    }

    public void detachPolicyFromRole(String roleName, String policyArn) {
        DetachRolePolicyRequest request = DetachRolePolicyRequest.builder()
                .roleName(roleName)
                .policyArn(policyArn)
                .build();
        iamClient.detachRolePolicy(request);
    }

    public List<String> listRolesByPrefix(String prefix) {
        ListRolesResponse response = iamClient.listRoles();
        return response.roles().stream()
                .filter(role -> role.roleName().startsWith(prefix))
                .map(Role::roleName)
                .collect(Collectors.toList());
    }
}
