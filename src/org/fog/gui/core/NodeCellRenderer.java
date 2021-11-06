package org.fog.gui.core;

import javax.swing.*;
import java.awt.*;

/** A cell renderer for the JComboBox when displaying a node object */
@SuppressWarnings("rawtypes")
public class NodeCellRenderer extends JLabel implements ListCellRenderer {
	private static final long serialVersionUID = 6021697923766790099L;

	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

		Node node = (Node) value;
		JLabel label = new JLabel();

		if (node != null && node.getName() != null) {
			label.setText(node.getName());
		}

		return label;
	}

}
