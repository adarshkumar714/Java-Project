import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;

public class StockMarketSimulator extends JFrame {

    private JTable marketTable, historyTable;
    private DefaultTableModel marketModel, historyModel;
    private JComboBox<String> stockSelector;
    private JTextField amountField;
    private JLabel balanceLabel, notificationLabel, totalProfitLabel;

    private Map<String, Double> stocks;
    private Map<String, Integer> portfolio;
    private Map<String, Double> avgBuyPrice;
    private double balance = 10000.00;
    private double totalProfit = 0.00;
    private javax.swing.Timer marketTimer;

    public StockMarketSimulator() {
        setTitle("Stock Market Simulator");
        setSize(1000, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(1, 3, 10, 10));

        // === Data ===
        stocks = new HashMap<>();
        stocks.put("TCS", 3200.00);
        stocks.put("INFY", 1500.00);
        stocks.put("RELIANCE", 2900.00);
        stocks.put("HDFC", 1650.00);
        stocks.put("SBIN", 780.00);

        portfolio = new HashMap<>();
        avgBuyPrice = new HashMap<>();

        // === Column 1: Buy/Sell Panel ===
        JPanel tradePanel = new JPanel(new GridLayout(9, 1, 10, 10));
        tradePanel.setBorder(new TitledBorder("Buy / Sell"));

        stockSelector = new JComboBox<>(stocks.keySet().toArray(new String[0]));
        amountField = new JTextField();
        balanceLabel = new JLabel("Balance: ₹" + String.format("%.2f", balance));
        totalProfitLabel = new JLabel("Total Profit: ₹" + String.format("%.2f", totalProfit));
        notificationLabel = new JLabel("Welcome to Stock Market Simulator!");
        notificationLabel.setForeground(Color.BLUE);

        JButton buyButton = new JButton("Buy");
        JButton sellButton = new JButton("Sell");

        buyButton.addActionListener(e -> buyStock());
        sellButton.addActionListener(e -> sellStock());

        tradePanel.add(new JLabel("Select Stock:"));
        tradePanel.add(stockSelector);
        tradePanel.add(new JLabel("Enter Quantity:"));
        tradePanel.add(amountField);
        tradePanel.add(balanceLabel);
        tradePanel.add(totalProfitLabel);
        tradePanel.add(buyButton);
        tradePanel.add(sellButton);
        tradePanel.add(notificationLabel);

        // === Column 2: Market Table ===
        JPanel marketPanel = new JPanel(new BorderLayout());
        marketPanel.setBorder(new TitledBorder("Market"));

        String[] marketCols = {"Stock", "Price (₹)"};
        marketModel = new DefaultTableModel(marketCols, 0);
        marketTable = new JTable(marketModel);
        marketTable.setEnabled(false);
        updateMarketTable();

        marketPanel.add(new JScrollPane(marketTable), BorderLayout.CENTER);

        // === Column 3: History Table ===
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBorder(new TitledBorder("History"));

        String[] columns = {"Action", "Stock", "Qty", "Price (₹)", "Total (₹)", "Profit/Loss (₹)"};
        historyModel = new DefaultTableModel(columns, 0);
        historyTable = new JTable(historyModel);
        historyTable.setEnabled(false);
        historyPanel.add(new JScrollPane(historyTable), BorderLayout.CENTER);

        // === Add panels to frame ===
        add(tradePanel);
        add(marketPanel);
        add(historyPanel);

        // === Timer for price updates ===
        marketTimer = new javax.swing.Timer(2000, e -> updatePrices());
        marketTimer.start(); // auto start
    }

    // --- Update market table with formatted prices ---
    private void updateMarketTable() {
        marketModel.setRowCount(0);
        for (String stock : stocks.keySet()) {
            marketModel.addRow(new Object[]{stock, String.format("%.2f", stocks.get(stock))});
        }
    }

    // --- Simulate price changes ---
    private void updatePrices() {
        Random rand = new Random();
        for (String stock : stocks.keySet()) {
            double price = stocks.get(stock);
            double change = (rand.nextDouble() - 0.5) * 100; // -50 to +50
            price = Math.max(1, price + change);
            stocks.put(stock, price);
        }
        updateMarketTable();
    }

    // --- Buy Stocks ---
    private void buyStock() {
        String stock = (String) stockSelector.getSelectedItem();
        String qtyText = amountField.getText();

        try {
            int qty = Integer.parseInt(qtyText);
            if (qty <= 0) throw new NumberFormatException();

            double price = stocks.get(stock);
            double totalCost = price * qty;

            if (balance >= totalCost) {
                balance -= totalCost;
                balanceLabel.setText("Balance: ₹" + String.format("%.2f", balance));

                int oldQty = portfolio.getOrDefault(stock, 0);
                double oldAvg = avgBuyPrice.getOrDefault(stock, 0.0);
                double newAvg = (oldAvg * oldQty + price * qty) / (oldQty + qty);

                portfolio.put(stock, oldQty + qty);
                avgBuyPrice.put(stock, newAvg);

                historyModel.addRow(new Object[]{
                    "Buy", stock, qty, 
                    String.format("%.2f", price), 
                    String.format("%.2f", totalCost), "-"
                });

                showNotification("✅ Bought " + qty + " " + stock, Color.GREEN.darker());
            } else {
                showNotification("❌ Insufficient balance!", Color.RED);
            }
        } catch (NumberFormatException ex) {
            showNotification("⚠️ Invalid quantity!", Color.ORANGE);
        }
    }

    // --- Sell Stocks ---
    private void sellStock() {
        String stock = (String) stockSelector.getSelectedItem();
        String qtyText = amountField.getText();

        try {
            int qty = Integer.parseInt(qtyText);
            if (qty <= 0) throw new NumberFormatException();

            int owned = portfolio.getOrDefault(stock, 0);
            if (owned >= qty) {
                double sellPrice = stocks.get(stock);
                double buyPrice = avgBuyPrice.getOrDefault(stock, sellPrice);
                double totalSell = sellPrice * qty;
                double profit = (sellPrice - buyPrice) * qty;

                balance += totalSell;
                balanceLabel.setText("Balance: ₹" + String.format("%.2f", balance));

                portfolio.put(stock, owned - qty);
                if (portfolio.get(stock) == 0) avgBuyPrice.remove(stock);

                totalProfit += profit;
                totalProfitLabel.setText("Total Profit: ₹" + String.format("%.2f", totalProfit));

                historyModel.addRow(new Object[]{
                    "Sell", stock, qty,
                    String.format("%.2f", sellPrice),
                    String.format("%.2f", totalSell),
                    String.format("%.2f", profit)
                });

                showNotification(
                    (profit >= 0 ? "💰 Profit ₹" : "📉 Loss ₹") + String.format("%.2f", profit),
                    profit >= 0 ? Color.GREEN.darker() : Color.RED
                );
            } else {
                showNotification("⚠️ Not enough shares to sell!", Color.RED);
            }
        } catch (NumberFormatException ex) {
            showNotification("⚠️ Invalid quantity!", Color.ORANGE);
        }
    }

    private void showNotification(String msg, Color color) {
        notificationLabel.setText(msg);
        notificationLabel.setForeground(color);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StockMarketSimulator().setVisible(true));
    }
}
