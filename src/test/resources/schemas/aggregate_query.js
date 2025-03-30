// MongoDB Aggregate Query
// This query joins orders and product_configs collections with specific conditions

db.orders.aggregate([
    // Match orders with specific conditions
    {
        $match: {
            status: { $in: ["PENDING", "PROCESSING", "CANCELLED"] },
            price: { $gt: 10 },
            orderDate: { 
                $gte: new Date(new Date().setDate(new Date().getDate() - 10))
            }
        }
    },
    
    // Lookup product configurations
    {
        $lookup: {
            from: "product_configs",
            localField: "productId",
            foreignField: "productId",
            as: "productConfig"
        }
    },
    
    // Unwind the product config array
    {
        $unwind: "$productConfig"
    },
    
    // Match product config conditions
    {
        $match: {
            "productConfig.enabled": true,
            "productConfig.startDate": { 
                $gte: new Date(new Date().setDate(new Date().getDate() - 10))
            },
            "productConfig.endDate": { 
                $lte: new Date(new Date().setDate(new Date().getDate() + 10))
            }
        }
    },
    
    // Group by product name
    {
        $group: {
            _id: "$productName",
            totalOrders: { $sum: 1 },
            totalQuantity: { $sum: "$quantity" },
            totalPrice: { $sum: { $multiply: ["$price", "$quantity"] } },
            averagePrice: { $avg: "$price" },
            statusCounts: {
                $push: {
                    status: "$status",
                    count: 1
                }
            }
        }
    },
    
    // Project final results
    {
        $project: {
            _id: 0,
            productName: "$_id",
            totalOrders: 1,
            totalQuantity: 1,
            totalPrice: 1,
            averagePrice: 1,
            statusBreakdown: {
                $map: {
                    input: "$statusCounts",
                    as: "status",
                    in: {
                        status: "$$status.status",
                        count: "$$status.count"
                    }
                }
            }
        }
    }
]); 