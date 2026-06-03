package gui;

import edu.memphis.ccrg.lida.framework.Agent;
import edu.memphis.ccrg.lida.framework.gui.panels.GuiPanelImpl;
import edu.memphis.ccrg.lida.framework.ModuleName;
import modules.Environment;
import javax.swing.*;
import java.awt.*;

public class ActionPanel extends GuiPanelImpl {

    private JLabel actionLabel;
    private Environment environment;

    public ActionPanel() {
        super();
        setLayout(new BorderLayout());
        actionLabel = new JLabel("...", SwingConstants.CENTER);
        actionLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        add(actionLabel, BorderLayout.CENTER);
    }

    @Override
    public void initPanel(String[] params) {}

    @Override
    public void registerAgent(Agent agent) {
        super.registerAgent(agent);
        environment = (Environment) agent.getSubmodule(ModuleName.getModuleName("Environment"));
    }

    @Override
    public void refresh() {
        if (environment != null) {
            String action = (String) environment.getModuleContent();
            actionLabel.setText(action != null ? action.toUpperCase() : "...");
        }
    }
}