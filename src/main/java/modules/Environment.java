package modules;

import edu.memphis.ccrg.lida.environment.EnvironmentImpl;
import edu.memphis.ccrg.lida.framework.ModuleName;
import edu.memphis.ccrg.lida.framework.tasks.FrameworkTaskImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import ws3dproxy.WS3DProxy;
import ws3dproxy.model.Creature;
import ws3dproxy.model.Leaflet;
import ws3dproxy.model.Thing;
import ws3dproxy.model.World;
import ws3dproxy.model.WorldPoint;
import ws3dproxy.util.Constants;

public class Environment extends EnvironmentImpl {

    private static final int DEFAULT_TICKS_PER_RUN = 100;
    private int ticksPerRun;
    private WS3DProxy proxy;
    private Creature creature;
    private Thing food;
    private Thing jewel;
    private List<Thing> thingAhead;
    private Thing leafletJewel;
    private String currentAction;   
    private boolean leafletReady;
    private WorldPoint deliverySpot;
    private List<Thing> visibleJewels;
    private List<Leaflet> leaflets;
    private PlanningModule planningModule;
    
    public Environment() {
        this.ticksPerRun = DEFAULT_TICKS_PER_RUN;
        this.proxy = new WS3DProxy();
        this.creature = null;
        this.food = null;
        this.jewel = null;
        this.thingAhead = new ArrayList<>();
        this.leafletJewel = null;
        this.currentAction = "rotate";
        this.leafletReady = false;
        this.deliverySpot = null;
        this.visibleJewels = new ArrayList<>();
        this.leaflets = new ArrayList<>();
    }

    @Override
    public void init() {
        super.init();
        planningModule = (PlanningModule) getSubmodule(ModuleName.getModuleName("PlanningModule"));
        ticksPerRun = (Integer) getParam("environment.ticksPerRun", DEFAULT_TICKS_PER_RUN);
        taskSpawner.addTask(new BackgroundTask(ticksPerRun));
        
        try {
            System.out.println("Reseting the WS3D World ...");
            proxy.getWorld().reset();
            creature = proxy.createCreature(100, 100, 0);
            creature.start();
            System.out.println("Starting the WS3D Resource Generator ... ");
            World.grow(1);
            Thread.sleep(4000);
            creature.updateState();
            System.out.println("DemoLIDA has started...");
            World.createDeliverySpot(
                proxy.getWorld().getEnvironmentWidth() / 2,
                proxy.getWorld().getEnvironmentHeight() / 2
            );
            deliverySpot = World.getDeliverySpot();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class BackgroundTask extends FrameworkTaskImpl {

        public BackgroundTask(int ticksPerRun) {
            super(ticksPerRun);
        }

        @Override
        protected void runThisFrameworkTask() {
            updateEnvironment();
            performAction(currentAction);
        }
    }

    @Override
    public void resetState() {
        currentAction = "rotate";
    }

    @Override
    public Object getState(Map<String, ?> params) {
        Object requestedObject = null;
        String mode = (String) params.get("mode");
        switch (mode) {
            case "food":
                requestedObject = food;
                break;
            case "jewel":
                requestedObject = jewel;
                break;
            case "thingAhead":
                requestedObject = thingAhead;
                break;
            case "leafletJewel":
                requestedObject = leafletJewel;
                break;
            case "leafletReady":
                requestedObject = leafletReady;
                break;
            case "deliverySpot":
                requestedObject = deliverySpot;
                break;
            case "visibleJewels":
                requestedObject = visibleJewels;
                break;
            case "leaflets":
                requestedObject = leaflets;
                break;
            default:
                break;
        }
        return requestedObject;
    }

    @Override
    public Object getModuleContent(Object... params) {
        return currentAction;
    }

    
    public void updateEnvironment() {
        creature.updateState();
        food = null;
        jewel = null;
        leafletJewel = null;
        leaflets = creature.getLeaflets();
        thingAhead.clear();
        visibleJewels.clear();

        for (Thing thing : creature.getThingsInVision()) {
            if (creature.calculateDistanceTo(thing) <= Constants.OFFSET) {
                // Identifica o objeto proximo
                thingAhead.add(thing);
                break;
            } else if (thing.getCategory() == Constants.categoryJEWEL) {
                visibleJewels.add(thing);
                if (planningModule != null) {
                    Thing target = planningModule.getTargetJewel();
                    if (target != null && thing.getName().equals(target.getName())) {
                        leafletJewel = thing;
                    }
                } else {
                    if (leafletJewel == null) {
                        for (Leaflet leaflet : creature.getLeaflets()) {
                            if (leaflet.ifInLeaflet(thing.getMaterial().getColorName()) &&
                                    leaflet.getTotalNumberOfType(thing.getMaterial().getColorName()) > leaflet.getCollectedNumberOfType(thing.getMaterial().getColorName())) {
                                leafletJewel = thing;
                                break;
                            }
                        }
                    }
                }
            } else if (food == null && creature.getFuel() <= 300.0
                        && (thing.getCategory() == Constants.categoryFOOD
                        || thing.getCategory() == Constants.categoryPFOOD
                        || thing.getCategory() == Constants.categoryNPFOOD)) {
                
                    // Identifica qualquer tipo de comida
                    food = thing;
            }
           
        }
        leafletReady = false;
        for (Leaflet leaflet : creature.getLeaflets()) {
            boolean complete = true;
            for (Integer[] counts : leaflet.getItems().values()) {
                if (counts[0] - counts[1] > 0) { complete = false; break; }
            }
            if (complete) { leafletReady = true; break; }
        }
    }
    
    
    
    @Override
    public void processAction(Object action) {
        String actionName = (String) action;
        currentAction = actionName.substring(actionName.indexOf(".") + 1);
    }

    private void performAction(String currentAction) {
        try {
            switch (currentAction) {
                case "rotate":
                    creature.rotate(3.0);
                    break;
                case "gotoFood":
                    if (food != null) 
                        creature.moveto(4.0, food.getX1(), food.getY1());
                    else creature.move(0.0, 0.0, 0.0);
                    break;
                case "gotoJewel":
                    if (leafletJewel != null)
                        creature.moveto(4.0, leafletJewel.getX1(), leafletJewel.getY1());
                    else creature.move(0.0, 0.0, 0.0);
                    break;                    
                case "get":
                    creature.move(0.0, 0.0, 0.0);
                    if (thingAhead != null) {
                        for (Thing thing : thingAhead) {
                            if (thing.getCategory() == Constants.categoryJEWEL) {
                                creature.putInSack(thing.getName());
                            } else if (thing.getCategory() == Constants.categoryFOOD || thing.getCategory() == Constants.categoryNPFOOD || thing.getCategory() == Constants.categoryPFOOD) {
                                creature.eatIt(thing.getName());
                            }
                        }
                    }
                    this.resetState();
                    break;
                case "gotoDeliverySpot":
                    if (deliverySpot != null)
                        creature.moveto(4.0, deliverySpot.getX(), deliverySpot.getY());
                    break;
                case "deliverLeaflet":
                    creature.move(0.0, 0.0, 0.0);
                    for (Leaflet leaflet : creature.getLeaflets()) {
                        if (planningModule != null && planningModule.isDelivered(leaflet.getID().toString())) continue;
                        boolean complete = true;
                        for (Integer[] counts : leaflet.getItems().values())
                            if (counts[0] - counts[1] > 0) { complete = false; break; }
                        if (complete) {
                            if (planningModule != null) planningModule.resetTarget();
                            creature.deliverLeaflet(leaflet.getID().toString());
                            System.out.println("[ENTREGA] Leaflet " + leaflet.getID() + " entregue!");
                            break;
                        }
                    }
                    this.resetState();
                    break;
                default:creature.move(0.0, 0.0, 0.0);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
