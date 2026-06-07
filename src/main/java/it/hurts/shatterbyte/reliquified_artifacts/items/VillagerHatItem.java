// ...

@SubscribeEvent
public void onTradeWithVillager(TradeWithVillagerEvent event) {
    if (event.getPlayer() == null) {
        return;
    }
    // existing code here
}

// ...