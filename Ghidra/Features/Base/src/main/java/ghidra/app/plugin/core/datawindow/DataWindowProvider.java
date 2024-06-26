/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ghidra.app.plugin.core.datawindow;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.table.TableColumnModel;

import docking.ActionContext;
import generic.theme.GIcon;
import ghidra.framework.plugintool.ComponentProviderAdapter;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Program;
import ghidra.util.HelpLocation;
import ghidra.util.table.*;

class DataWindowProvider extends ComponentProviderAdapter {

	public static final Icon ICON = new GIcon("icon.plugin.datawindow.provider");

	private DataWindowPlugin plugin;

	private GhidraThreadedTablePanel<DataRowObject> threadedTablePanel;
	private GhidraTableFilterPanel<DataRowObject> filterPanel;
	private JComponent mainPanel;

	private GhidraTable dataTable;
	private DataTableModel dataModel;

	DataWindowProvider(DataWindowPlugin plugin) {
		super(plugin.getTool(), "Data Window", plugin.getName());
		setTitle("Defined Data");
		this.plugin = plugin;
		mainPanel = createWorkPanel();
		tool.addComponentProvider(this, false);
		setIcon(ICON);
	}

	@Override
	public void componentHidden() {
		dataModel.reload(null);
	}

	@Override
	public void componentShown() {
		plugin.dataWindowShown();
		dataModel.reload(plugin.getProgram());
	}

	@Override
	public ActionContext getActionContext(MouseEvent event) {
		return new DataWindowContext(this, dataTable);
	}

	@Override
	public JComponent getComponent() {
		return mainPanel;
	}

	@Override
	public HelpLocation getHelpLocation() {
		return new HelpLocation(plugin.getName(), plugin.getName());
	}

	void programOpened(Program program) {
		if (isVisible()) {
			dataModel.reload(program);
		}
	}

	void programClosed() {
		if (isVisible()) {
			dataModel.reload(null);
		}
	}

	void dispose() {
		tool.removeComponentProvider(this);
		threadedTablePanel.dispose();
		filterPanel.dispose();
	}

	private JComponent createWorkPanel() {

		dataModel = new DataTableModel(plugin);

		threadedTablePanel = new GhidraThreadedTablePanel<>(dataModel, 1000);
		dataTable = threadedTablePanel.getTable();
		dataTable.setAutoLookupColumn(DataTableModel.DATA_COL);
		dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		dataTable.setPreferredScrollableViewportSize(new Dimension(350, 150));
		dataTable.setRowSelectionAllowed(true);
		dataTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		dataTable.getSelectionModel().addListSelectionListener(e -> notifyContextChanged());

		dataModel.addTableModelListener(e -> {
			int rowCount = dataModel.getRowCount();
			int unfilteredCount = dataModel.getUnfilteredRowCount();

			StringBuilder buffy = new StringBuilder();

			buffy.append(rowCount).append(" items");
			if (rowCount != unfilteredCount) {
				buffy.append(" (of ").append(unfilteredCount).append(" )");
			}

			setSubTitle(buffy.toString());
		});

		dataTable.installNavigation(tool);

		setDataTableRenderer();

		filterPanel = new GhidraTableFilterPanel<>(dataTable, dataModel);

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(threadedTablePanel, BorderLayout.CENTER);
		panel.add(filterPanel, BorderLayout.SOUTH);

		String namePrefix = "Defined Data";
		dataTable.setAccessibleNamePrefix(namePrefix);
		filterPanel.setAccessibleNamePrefix(namePrefix);

		return panel;
	}

	private void notifyContextChanged() {
		tool.contextChanged(this);
	}

	private void setDataTableRenderer() {
		TableColumnModel columnModel = dataTable.getColumnModel();
		columnModel.getColumn(DataTableModel.LOCATION_COL)
				.setPreferredWidth(DataTableModel.ADDRESS_COL_WIDTH);
		columnModel.getColumn(DataTableModel.SIZE_COL)
				.setPreferredWidth(DataTableModel.SIZE_COL_WIDTH);
	}

	void reload() {
		if (isVisible()) {
			dataModel.reload(plugin.getProgram());
		}
	}

	void dataAdded(Address loc) {
		if (isVisible()) {
			dataModel.dataAdded(loc);
		}
	}

	GhidraTable getTable() {
		return dataTable;
	}
}
