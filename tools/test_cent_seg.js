const https = require('https');

const segUrl = 'https://falling-bar-0032.9-c22.workers.dev/b?z=lvxqFkpEhZTKGl7ywuQSqbITac04ZrQ-8VvIVHociiXccsNr7Q2dIHCaSHbeIvn42ICV57jggPK8YPB4P4fCe9Fz14KC5E8l8TSenQE1CZLIRNRxk-dzQzutz9JyKXe51hR-EbQ6p2Y&x=init-stream3.m4s&h=PkT5Jlww1tgLYobodU0XL8G9-g8qyDFKcIsXCoAmzhPi5PuCUtZooiuBvvXp6YE3ubOq_ycCuGaBO7pnVDPq9f0DnhvgqmIQkeqIbfDo6ic7eUj6ujlHB8kSi5_WOUe8yYkvKmAqr1jinA_EiM2niX2CszFBw4JUdvOMpq-bVtsVObp2a0BnZbXKoKe4cIOlmXWyF-2MPAlkDbygxaS2ynib22aPNIK2hhcutDfhcgPd9s5oDZ-oPSfwgRD0IIPbzd8bKF-WMCaS53RCohJ2D2-kK773t3Q28mnIWvtkdtMTZOA7i_7Zcg4s9nSRupQXQyR7zTgTVKXvfJhUwYjo-M9B3Kz96oVJY9WZu61gbq0OSCz7ij8OK6cMO7sZ_IRAIF7XvrBbMoM9IxuJNl2fs_dzoUBehoc1RSOa8vA3PceJG9PW7l0zrSA4ahhpaK6nXj4je93RUf5NjZMbpmYth5OF0sd4V7EyhA3O1JbMr9FNpdtMQIidPliOJ5hKhSBZiEbu9n1K2RYpwpN2twiFd0HdiBOk6VtIE16jHXUeXWIEotTNKe1aDZEkdAXm9IIVU_aU7kmzESKX8qlLUhI5zqgMfGZCQF9oq3lTDxjWDB26qM3dwQmkIvgXYJcnh7z9a0Ys';

https.get(segUrl, {
    headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        'Referer': 'https://vidstuck.xyz/'
    }
}, res => {
    console.log('Status:', res.statusCode);
    console.log('Headers:', res.headers);
    let chunks = [];
    res.on('data', c => chunks.push(c));
    res.on('end', () => {
        const buf = Buffer.concat(chunks);
        console.log('Length:', buf.length);
        console.log('First 16 bytes (hex):', buf.subarray(0, 16).toString('hex'));
        console.log('First 16 bytes (ascii):', buf.subarray(0, 16).toString('ascii'));
    });
});
