package com.hmall.order.acceptance;

import com.hmall.order.acceptance.config.OccupyInventoryStub;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;

import static org.assertj.core.api.Assertions.assertThat;

public class InventoryStubStepDefinitions {

    private final OccupyInventoryStub occupyInventoryStub;

    public InventoryStubStepDefinitions(OccupyInventoryStub occupyInventoryStub) {
        this.occupyInventoryStub = occupyInventoryStub;
    }

    @Before
    public void resetStub() {
        occupyInventoryStub.reset();
    }

    @Given("Inventory 桩配置为返回库存不足")
    public void inventory桩配置为返回库存不足() {
        occupyInventoryStub.setShouldFail(true);
    }

    @And("Inventory 占用不应被调用")
    public void inventory占用不应被调用() {
        assertThat(occupyInventoryStub.getCallCount()).isEqualTo(0);
    }

    @And("Inventory 占用应仅收到 {int} 条明细")
    public void inventory占用应仅收到N条明细(int expectedCount) {
        assertThat(occupyInventoryStub.getCallCount()).isEqualTo(1);
        assertThat(occupyInventoryStub.getLastItemCount()).isEqualTo(expectedCount);
    }
}
