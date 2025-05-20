import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class CarRentalGUI extends JFrame {
    private CarRentalSystem rentalSystem;
    private JTable carTable;
    private DefaultTableModel tableModel;
    private JTextField customerNameField;
    private JTextField daysField;
    private JLabel totalLabel;
    
    public CarRentalGUI(CarRentalSystem rentalSystem) {
        this.rentalSystem = rentalSystem;
        setTitle("Car Rental System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        // Create main panel with tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Rent a Car", createRentPanel());
        tabbedPane.addTab("Return a Car", createReturnPanel());
        
        add(tabbedPane);
    }
    
    private JPanel createRentPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create table
        String[] columns = {"Car ID", "Brand", "Model", "Price per Day", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        carTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(carTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Create input panel
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Customer name input
        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(new JLabel("Customer Name:"), gbc);
        gbc.gridx = 1;
        customerNameField = new JTextField(20);
        inputPanel.add(customerNameField, gbc);
        
        // Rental days input
        gbc.gridx = 0; gbc.gridy = 1;
        inputPanel.add(new JLabel("Rental Days:"), gbc);
        gbc.gridx = 1;
        daysField = new JTextField(10);
        inputPanel.add(daysField, gbc);
        
        // Total price label
        gbc.gridx = 0; gbc.gridy = 2;
        inputPanel.add(new JLabel("Total Price:"), gbc);
        gbc.gridx = 1;
        totalLabel = new JLabel("$0.00");
        inputPanel.add(totalLabel, gbc);
        
        // Rent button
        gbc.gridx = 1; gbc.gridy = 3;
        JButton rentButton = new JButton("Rent Car");
        rentButton.addActionListener(e -> rentCar());
        inputPanel.add(rentButton, gbc);
        
        // Refresh button
        gbc.gridx = 0; gbc.gridy = 3;
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshCarList());
        inputPanel.add(refreshButton, gbc);
        
        panel.add(inputPanel, BorderLayout.SOUTH);
        
        // Initial car list load
        refreshCarList();
        
        // Add listener for table selection
        carTable.getSelectionModel().addListSelectionListener(e -> updateTotalPrice());
        
        return panel;
    }
    
    private JPanel createReturnPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create table for rented cars
        String[] columns = {"Car ID", "Brand", "Model", "Customer", "Days"};
        DefaultTableModel rentedTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable rentedCarsTable = new JTable(rentedTableModel);
        JScrollPane scrollPane = new JScrollPane(rentedCarsTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Return button
        JButton returnButton = new JButton("Return Selected Car");
        returnButton.addActionListener(e -> returnCar(rentedCarsTable));
        
        // Refresh button
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshRentedCarList(rentedTableModel));
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(returnButton);
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Initial load of rented cars
        refreshRentedCarList(rentedTableModel);
        
        return panel;
    }
    
    private void refreshCarList() {
        tableModel.setRowCount(0);
        for (Car car : rentalSystem.getCars()) {
            tableModel.addRow(new Object[]{
                car.getCarId(),
                car.getBrand(),
                car.getModel(),
                String.format("$%.2f", car.getBasePricePerDay()),
                car.isAvailable() ? "Available" : "Rented"
            });
        }
    }
    
    private void refreshRentedCarList(DefaultTableModel model) {
        model.setRowCount(0);
        for (Rental rental : rentalSystem.getRentals()) {
            Car car = rental.getCar();
            Customer customer = rental.getCustomer();
            model.addRow(new Object[]{
                car.getCarId(),
                car.getBrand(),
                car.getModel(),
                customer.getName(),
                rental.getDays()
            });
        }
    }
    
    private void updateTotalPrice() {
        int selectedRow = carTable.getSelectedRow();
        if (selectedRow >= 0 && !daysField.getText().isEmpty()) {
            try {
                int days = Integer.parseInt(daysField.getText());
                String carId = (String) carTable.getValueAt(selectedRow, 0);
                Car selectedCar = rentalSystem.findCarById(carId);
                if (selectedCar != null) {
                    double total = selectedCar.calculatePrice(days);
                    totalLabel.setText(String.format("$%.2f", total));
                    return;
                }
            } catch (NumberFormatException ex) {
                // Invalid number of days
            }
        }
        totalLabel.setText("$0.00");
    }
    
    private void rentCar() {
        int selectedRow = carTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a car to rent.");
            return;
        }
        
        String carId = (String) carTable.getValueAt(selectedRow, 0);
        Car selectedCar = rentalSystem.findCarById(carId);
        
        if (selectedCar == null || !selectedCar.isAvailable()) {
            JOptionPane.showMessageDialog(this, "Selected car is not available.");
            return;
        }
        
        String customerName = customerNameField.getText().trim();
        if (customerName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter customer name.");
            return;
        }
        
        try {
            int days = Integer.parseInt(daysField.getText().trim());
            if (days <= 0) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number of days.");
                return;
            }
            
            Customer newCustomer = new Customer("CUS" + (rentalSystem.getCustomers().size() + 1), customerName);
            rentalSystem.addCustomer(newCustomer);
            rentalSystem.rentCar(selectedCar, newCustomer, days);
            
            JOptionPane.showMessageDialog(this, "Car rented successfully!");
            customerNameField.setText("");
            daysField.setText("");
            refreshCarList();
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number of days.");
        }
    }
    
    private void returnCar(JTable rentedCarsTable) {
        int selectedRow = rentedCarsTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a car to return.");
            return;
        }
        
        String carId = (String) rentedCarsTable.getValueAt(selectedRow, 0);
        Car carToReturn = rentalSystem.findCarById(carId);
        
        if (carToReturn != null) {
            rentalSystem.returnCar(carToReturn);
            JOptionPane.showMessageDialog(this, "Car returned successfully!");
            refreshCarList();
            refreshRentedCarList((DefaultTableModel) rentedCarsTable.getModel());
        }
    }
}
