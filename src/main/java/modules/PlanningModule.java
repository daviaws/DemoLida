package modules;

import edu.memphis.ccrg.lida.framework.FrameworkModuleImpl;
import edu.memphis.ccrg.lida.framework.ModuleName;
import edu.memphis.ccrg.lida.globalworkspace.BroadcastListener;
import edu.memphis.ccrg.lida.globalworkspace.Coalition;
import edu.memphis.ccrg.lida.sensorymemory.SensoryMemory;
import java.util.List;
import java.util.Map;
import ws3dproxy.model.Leaflet;
import ws3dproxy.model.Thing;
import ws3dproxy.util.Constants;

public class PlanningModule extends FrameworkModuleImpl implements BroadcastListener {

    private SensoryMemory sensoryMemory;
    private Leaflet targetLeaflet;
    private Thing targetJewel;
    private final Map<String, Object> smParams = new java.util.HashMap<>();
    private final java.util.Set<String> deliveredLeaflets = new java.util.HashSet<>();

    @Override
    public void init() {
        super.init();
        sensoryMemory = (SensoryMemory) getSubmodule(ModuleName.getModuleName("SensoryMemory"));
    }

    @Override
    public void receiveBroadcast(Coalition coalition) {
        updatePlan();
    }

    @Override
    public void learn(Coalition coalition) {}

    public boolean isDelivered(String leafletId) {
        return deliveredLeaflets.contains(leafletId);
    }

    private void updatePlan() {
        smParams.clear();
        smParams.put("mode", "leafletReady");
        Boolean ready = (Boolean) sensoryMemory.getSensoryContent("", smParams);
        if (ready != null && ready) {
            targetJewel = null;
            return;
        }

        smParams.clear();
        smParams.put("mode", "visibleJewels");
        @SuppressWarnings("unchecked")
        List<Thing> visibleJewels = (List<Thing>) sensoryMemory.getSensoryContent("", smParams);
        if (visibleJewels == null || visibleJewels.isEmpty()) {
            targetJewel = null;
            return;
        }

        smParams.clear();
        smParams.put("mode", "leaflets");
        @SuppressWarnings("unchecked")
        List<Leaflet> leaflets = (List<Leaflet>) sensoryMemory.getSensoryContent("", smParams);
        if (leaflets == null || leaflets.isEmpty()) {
            targetJewel = null;
            return;
        }

        if (targetLeaflet == null || isComplete(targetLeaflet) || !isFeasible(targetLeaflet, visibleJewels)) {
            targetLeaflet = selectBestLeaflet(leaflets, visibleJewels);
            targetJewel = null;
            System.out.println("[PLANO] Novo leaflet alvo: " + (targetLeaflet != null ? targetLeaflet.getID() : "nenhum"));
        }
        if (targetLeaflet == null) {
            targetJewel = null;
            return;
        }

        if (targetJewel != null) {
            boolean stillVisible = visibleJewels.stream()
                .anyMatch(t -> t.getName().equals(targetJewel.getName()));
            if (!stillVisible) targetJewel = null;
        }
        if (targetJewel == null) {
            targetJewel = selectTargetJewel(leaflets, visibleJewels);
        }
    }

    private Leaflet selectBestLeaflet(List<Leaflet> leaflets, List<Thing> visibleJewels) {
        Leaflet best = null;
        double bestPayment = -1;
        for (Leaflet l : leaflets) {
            if (deliveredLeaflets.contains(l.getID().toString())) continue;
            if (isComplete(l)) continue;
            if (!isFeasible(l, visibleJewels)) continue;
            if (l.getPayment() > bestPayment) {
                bestPayment = l.getPayment();
                best = l;
            }
        }
        return best;
    }

    private Thing selectTargetJewel(List<Leaflet> leaflets, List<Thing> visibleJewels) {
        // Conta quantas joias de cada cor ainda faltam no total de todos os leaflets
        Map<String, Integer> needed = new java.util.HashMap<>();
        for (Leaflet l : leaflets) {
            if (deliveredLeaflets.contains(l.getID().toString())) continue;
            for (Map.Entry<String, Integer[]> entry : l.getItems().entrySet()) {
                String color = entry.getKey();
                int missing = entry.getValue()[0] - entry.getValue()[1];
                if (missing > 0)
                    needed.merge(color, missing, Integer::sum);
            }
        }

        for (Thing t : visibleJewels) {
            String color = Constants.getColorName(t.getMaterial().getColor());
            if (needed.getOrDefault(color, 0) > 0) {
                System.out.println("[PLANO] Alvo: " + t.getName() + " (" + color + ") faltam=" + needed.get(color));
                return t;
            }
        }
        return null;
    }

    private boolean isComplete(Leaflet l) {
        for (Integer[] counts : l.getItems().values())
            if (counts[0] - counts[1] > 0) return false;
        return true;
    }

    private boolean isFeasible(Leaflet l, List<Thing> visibleJewels) {
        for (Map.Entry<String, Integer[]> entry : l.getItems().entrySet()) {
            if (entry.getValue()[0] - entry.getValue()[1] <= 0) continue;
            boolean found = false;
            for (Thing t : visibleJewels)
                if (Constants.getColorName(t.getMaterial().getColor()).equals(entry.getKey())) { found = true; break; }
            if (!found) return false;
        }
        return true;
    }

    public Thing getTargetJewel() { return targetJewel; }

    public void resetTarget() {
        if (targetLeaflet != null) {
            deliveredLeaflets.add(targetLeaflet.getID().toString());
            System.out.println("[PLANO] Leaflet " + targetLeaflet.getID() + " marcado como entregue.");
        }
        targetLeaflet = null;
        targetJewel = null;
    }

    @Override
    public Object getModuleContent(Object... params) { return targetJewel; }

    @Override
    public void decayModule(long ticks) {}
}