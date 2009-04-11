package jd.gui.skins.simple;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import jd.gui.skins.simple.config.ConfigEntriesPanel;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingworker.SwingWorker;
import org.jdesktop.swingx.JXTaskPane;

public class JDCollapser extends JXTaskPane implements MouseListener {
    private static JDCollapser INSTANCE = null;

    public static JDCollapser getInstance() {
        if (INSTANCE == null) INSTANCE = new JDCollapser();
        return INSTANCE;
    }

    private JTabbedPanel panel;

    private JDCollapser() {
        super();
        this.setVisible(false);
        this.setCollapsed(true);
        this.addMouseListener(this);
    }

    public void mouseClicked(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mousePressed(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mouseReleased(MouseEvent e) {
        if (this.getContentPane().getComponent(0) instanceof ConfigEntriesPanel) {

            ((ConfigEntriesPanel) this.getContentPane().getComponent(0)).save();
            ((ConfigEntriesPanel) this.getContentPane().getComponent(0)).saveConfigEntries();
        }
        this.setCollapsed(true);
        new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                Thread.sleep(500);
                return null;
            }

            protected void done() {
                setVisible(false);
            }
        }.execute();

    }

    public void setContentPanel(JTabbedPanel panel) {
        if (panel == this.panel) return;

        if (this.panel != null) {
            this.panel.onHide();
            getContentPane().remove(this.panel);
        }

        this.panel = panel;
        panel.onDisplay();
        getContentPane().setLayout(new MigLayout("ins 0,wrap 1", "[grow, fill]", "[grow,fill]"));
        getContentPane().add(this.panel, "cell 0 0");
        this.invalidate();
        this.repaint();

    }
}
