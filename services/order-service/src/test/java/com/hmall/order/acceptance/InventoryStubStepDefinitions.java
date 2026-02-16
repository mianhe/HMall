package com.hmall.order.acceptance;

import com.hmall.order.acceptance.config.OccupyInventoryStub;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;

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
}
