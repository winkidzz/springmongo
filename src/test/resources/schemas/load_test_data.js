// Clear existing data
db.orders.drop();
db.product_configs.drop();

// Insert product configurations
const productConfigs = [];
const now = new Date();
const startDate = new Date(now.getTime() - (10 * 24 * 60 * 60 * 1000)); // 10 days ago
const endDate = new Date(now.getTime() + (10 * 24 * 60 * 60 * 1000));   // 10 days ahead

// Create 5 product configurations
for (let i = 0; i < 5; i++) {
    productConfigs.push({
        productId: `PROD${i}`,
        configName: `Config${i}`,
        configValue: `Value${i}`,
        startDate: startDate,
        endDate: endDate,
        enabled: true
    });
}

db.product_configs.insertMany(productConfigs);

// Insert orders
const orders = [];
const statuses = ['PENDING', 'PROCESSING', 'CANCELLED'];
const productNames = ['Product A', 'Product B', 'Product C', 'Product D', 'Product E'];

// Create 50 orders
for (let i = 0; i < 50; i++) {
    const productId = `PROD${Math.floor(Math.random() * 5)}`;
    const productName = productNames[Math.floor(Math.random() * productNames.length)];
    const price = Math.random() * 100 + 10; // Random price between 10 and 110
    const quantity = Math.floor(Math.random() * 10) + 1; // Random quantity between 1 and 10
    const orderDate = new Date(startDate.getTime() + Math.random() * (endDate.getTime() - startDate.getTime()));
    
    orders.push({
        productId: productId,
        orderNumber: `ORD${i}`,
        orderDate: orderDate,
        productName: productName,
        productCategory: `Category${Math.floor(Math.random() * 5)}`,
        price: price,
        quantity: quantity,
        status: statuses[Math.floor(Math.random() * statuses.length)]
    });
}

db.orders.insertMany(orders);

print('Test data inserted successfully');
print(`Orders count: ${db.orders.countDocuments()}`);
print(`Product configs count: ${db.product_configs.countDocuments()}`); 