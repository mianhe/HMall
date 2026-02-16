package com.hmall.user.acceptance;

import com.hmall.user.acceptance.config.LastResponseContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class AddressStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final LastResponseContext lastResponseContext;
    private final Map<String, Long> usernameToId;
    private Long lastAddressId;

    private ResponseEntity<AddressApiDto.Response> lastAddressResponse;
    private ResponseEntity<List<AddressApiDto.Response>> lastAddressListResponse;
    private ResponseEntity<Void> lastDeleteResponse;

    public AddressStepDefinitions(TestRestTemplate restTemplate, LastResponseContext lastResponseContext,
                                   UserStepDefinitions userStepDefinitions) {
        this.restTemplate = restTemplate;
        this.lastResponseContext = lastResponseContext;
        this.usernameToId = userStepDefinitions.getUsernameToId();
    }

    private void setLastStatusCode(int statusCode) {
        lastResponseContext.setLastStatusCode(statusCode);
    }

    private Long getUserId(String username) {
        Long id = usernameToId.get(username);
        assertThat(id).as("用户「%s」应先存在", username).isNotNull();
        return id;
    }

    @When("用户 {string} 新增收货地址 {string} {string} {string} {string} {string} {string}")
    public void 用户新增收货地址(String username, String recipientName, String phone,
                                 String province, String city, String district, String detail) {
        long userId = getUserId(username);
        AddressApiDto.Create body = new AddressApiDto.Create();
        body.recipientName = recipientName;
        body.phone = phone;
        body.province = province;
        body.city = city;
        body.district = district;
        body.detail = detail;
        lastAddressResponse = AddressApiDto.postAddress(restTemplate, userId, body);
        setLastStatusCode(lastAddressResponse.getStatusCode().value());
        if (lastAddressResponse.getStatusCode().is2xxSuccessful() && lastAddressResponse.getBody() != null) {
            lastAddressId = lastAddressResponse.getBody().addressId;
        }
    }

    @And("返回的 addressId 不为空")
    public void 返回的addressId不为空() {
        assertThat(lastAddressResponse.getBody()).isNotNull();
        assertThat(lastAddressResponse.getBody().addressId).isNotNull();
    }

    @And("返回的地址包含 recipientName {string}")
    public void 返回的地址包含recipientName(String expected) {
        assertThat(lastAddressResponse.getBody()).isNotNull();
        assertThat(lastAddressResponse.getBody().recipientName).isEqualTo(expected);
    }

    @When("用户请求用户 {string} 的地址列表")
    public void 用户请求地址列表(String username) {
        long userId = getUserId(username);
        lastAddressListResponse = AddressApiDto.getAddresses(restTemplate, userId);
        setLastStatusCode(lastAddressListResponse.getStatusCode().value());
    }

    @And("返回的地址列表至少有 {int} 个")
    public void 返回的地址列表至少有(int minCount) {
        assertThat(lastAddressListResponse).isNotNull();
        assertThat(lastAddressListResponse.getBody()).hasSizeGreaterThanOrEqualTo(minCount);
    }

    @And("返回的地址列表中包含 recipientName {string}")
    public void 返回的地址列表中包含recipientName(String expected) {
        List<AddressApiDto.Response> list = lastAddressListResponse.getBody();
        assertThat(list).isNotNull();
        boolean found = list.stream().anyMatch(a -> expected.equals(a.recipientName));
        assertThat(found).as("地址列表中应包含 recipientName: %s", expected).isTrue();
    }

    @When("用户 {string} 请求刚创建的地址详情")
    public void 用户请求刚创建的地址详情(String username) {
        long userId = getUserId(username);
        assertThat(lastAddressId).isNotNull();
        lastAddressResponse = AddressApiDto.getAddress(restTemplate, userId, lastAddressId);
        setLastStatusCode(lastAddressResponse.getStatusCode().value());
    }

    @And("返回的地址包含 detail {string}")
    public void 返回的地址包含detail(String expected) {
        assertThat(lastAddressResponse.getBody()).isNotNull();
        assertThat(lastAddressResponse.getBody().detail).isEqualTo(expected);
    }

    @When("用户 {string} 将刚创建的地址修改为 recipientName {string} phone {string} province {string} city {string} district {string} detail {string}")
    public void 用户将地址修改为(String username, String recipientName, String phone,
                                  String province, String city, String district, String detail) {
        long userId = getUserId(username);
        assertThat(lastAddressId).isNotNull();
        AddressApiDto.Update body = new AddressApiDto.Update();
        body.recipientName = recipientName;
        body.phone = phone;
        body.province = province;
        body.city = city;
        body.district = district;
        body.detail = detail;
        lastAddressResponse = AddressApiDto.putAddress(restTemplate, userId, lastAddressId, body);
        setLastStatusCode(lastAddressResponse.getStatusCode().value());
    }

    @And("返回的地址包含 phone {string}")
    public void 返回的地址包含phone(String expected) {
        assertThat(lastAddressResponse.getBody()).isNotNull();
        assertThat(lastAddressResponse.getBody().phone).isEqualTo(expected);
    }

    @When("用户 {string} 删除刚创建的地址")
    public void 用户删除刚创建的地址(String username) {
        long userId = getUserId(username);
        assertThat(lastAddressId).isNotNull();
        lastDeleteResponse = AddressApiDto.deleteAddress(restTemplate, userId, lastAddressId);
        setLastStatusCode(lastDeleteResponse.getStatusCode().value());
    }

    @When("用户 {string} 请求地址 ID {long} 的详情")
    public void 用户请求地址ID详情(String username, long addressId) {
        long userId = getUserId(username);
        lastAddressResponse = AddressApiDto.getAddress(restTemplate, userId, addressId);
        setLastStatusCode(lastAddressResponse.getStatusCode().value());
    }

    @And("返回的地址列表中不应包含 recipientName {string}")
    public void 返回的地址列表中不应包含recipientName(String expected) {
        List<AddressApiDto.Response> list = lastAddressListResponse.getBody();
        assertThat(list).isNotNull();
        boolean found = list.stream().anyMatch(a -> expected.equals(a.recipientName));
        assertThat(found).as("地址列表中不应包含 recipientName: %s", expected).isFalse();
    }
}
