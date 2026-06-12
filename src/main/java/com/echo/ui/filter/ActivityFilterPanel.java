package com.echo.ui.filter;

import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.echo.domain.EnhancedRoster;
import com.echo.filter.ActivityFilter;
import com.echo.service.ActivityCatalog;

/**
 * Sidebar panel for the {@link ActivityFilter} (B2) — a conventional multi-select filter.
 *
 * A "Round" scope combo (Any / R1 / R2 / R3) above a plain checkbox list of the derived activity
 * catalog. No chips, no inner scroll — the sidebar scrolls as a whole, like every other filter panel.
 */
public class ActivityFilterPanel {

    private ActivityFilterPanel() {
        // built via the static factory
    }

    /**
     * Builds the sidebar panel for the given filter and roster.
     */
    public static CollapsibleFilterPanel build(ActivityFilter filter, EnhancedRoster roster) {
        CollapsibleFilterPanel panel = new CollapsibleFilterPanel(ActivityFilter.FILTER_NAME);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        // Match the shared expanded-panel tone used by FilterPanelFactory; this panel is hand-built
        // and would otherwise inherit the lighter default panel color.
        content.setBackground(FilterSidebar.FILTER_COLOR_EXPANDED);

        // Round scope: index 0 = Any (ROUND_ANY), 1..3 = that round
        JComboBox<String> roundCombo = new JComboBox<>(new String[] {"Any", "R1", "R2", "R3"});
        roundCombo.setSelectedIndex(filter.getRoundScope());
        roundCombo.addActionListener(e -> {
            filter.setRoundScope(roundCombo.getSelectedIndex());
            panel.notifyFilterChanged();
        });
        JPanel roundRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        roundRow.setBackground(FilterSidebar.FILTER_COLOR_EXPANDED);
        roundRow.add(new JLabel("Round:"));
        roundRow.add(roundCombo);
        content.add(leftAligned(roundRow));

        // Plain checkbox list of catalog activities (no scroll pane; the sidebar scrolls)
        for (String activity : ActivityCatalog.build(roster)) {
            JCheckBox box = new JCheckBox(activity);
            box.setBackground(FilterSidebar.FILTER_COLOR_EXPANDED);
            box.setSelected(filter.getSelectedActivities().contains(activity));
            box.addActionListener(e -> {
                if (box.isSelected()) {
                    filter.addActivity(activity);
                } else {
                    filter.removeActivity(activity);
                }
                panel.notifyFilterChanged();
            });
            content.add(leftAligned(box));
        }

        panel.addContent(content);
        return panel;
    }

    private static JPanel leftAligned(Component c) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        wrapper.setBackground(FilterSidebar.FILTER_COLOR_EXPANDED);
        wrapper.add(c);
        return wrapper;
    }
}
