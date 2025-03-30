// Performance test for the aggregation query

function runPerformanceTest() {
    const iterations = 10;
    const results = [];
    
    print('Starting performance test...');
    print(`Number of iterations: ${iterations}`);
    
    for (let i = 0; i < iterations; i++) {
        const startTime = new Date();
        
        // Run the aggregation query
        const result = db.orders.aggregate([
            {
                $match: {
                    status: { $in: ['PENDING', 'PROCESSING', 'CANCELLED'] },
                    price: { $gt: 10 },
                    orderDate: { 
                        $gte: new Date(new Date().setDate(new Date().getDate() - 10))
                    }
                }
            },
            {
                $lookup: {
                    from: 'product_configs',
                    localField: 'productId',
                    foreignField: 'productId',
                    as: 'productConfig'
                }
            },
            {
                $unwind: '$productConfig'
            },
            {
                $match: {
                    'productConfig.enabled': true,
                    'productConfig.startDate': { 
                        $gte: new Date(new Date().setDate(new Date().getDate() - 10))
                    },
                    'productConfig.endDate': { 
                        $lte: new Date(new Date().setDate(new Date().getDate() + 10))
                    }
                }
            },
            {
                $group: {
                    _id: '$productName',
                    totalOrders: { $sum: 1 },
                    totalQuantity: { $sum: '$quantity' },
                    totalPrice: { $sum: { $multiply: ['$price', '$quantity'] } },
                    averagePrice: { $avg: '$price' },
                    statusCounts: {
                        $push: {
                            status: '$status',
                            count: 1
                        }
                    }
                }
            },
            {
                $project: {
                    _id: 0,
                    productName: '$_id',
                    totalOrders: 1,
                    totalQuantity: 1,
                    totalPrice: 1,
                    averagePrice: 1,
                    statusBreakdown: {
                        $map: {
                            input: '$statusCounts',
                            as: 'status',
                            in: {
                                status: '$$status.status',
                                count: '$$status.count'
                            }
                        }
                    }
                }
            }
        ]).toArray();
        
        const endTime = new Date();
        const executionTime = endTime - startTime;
        
        results.push({
            iteration: i + 1,
            executionTime: executionTime,
            resultCount: result.length
        });
        
        print(`Iteration ${i + 1}:`);
        print(`  Execution time: ${executionTime}ms`);
        print(`  Number of results: ${result.length}`);
    }
    
    // Calculate statistics
    const avgTime = results.reduce((sum, r) => sum + r.executionTime, 0) / iterations;
    const minTime = Math.min(...results.map(r => r.executionTime));
    const maxTime = Math.max(...results.map(r => r.executionTime));
    
    print('\nPerformance Summary:');
    print(`Average execution time: ${avgTime.toFixed(2)}ms`);
    print(`Minimum execution time: ${minTime}ms`);
    print(`Maximum execution time: ${maxTime}ms`);
    print(`Average number of results: ${(results.reduce((sum, r) => sum + r.resultCount, 0) / iterations).toFixed(2)}`);
}

// Run the test
runPerformanceTest(); 