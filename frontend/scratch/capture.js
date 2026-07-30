const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch({ headless: 'new' });
  const page = await browser.newPage();
  
  page.on('console', msg => {
    console.log(`[BROWSER CONSOLE] ${msg.type().toUpperCase()}: ${msg.text()}`);
  });

  page.on('pageerror', err => {
    console.log(`[BROWSER EXCEPTION]: ${err.message}`);
  });

  page.on('requestfailed', request => {
    console.log(`[NETWORK ERROR] ${request.url()} - ${request.failure().errorText}`);
  });

  page.on('response', response => {
    if (!response.ok()) {
      console.log(`[NETWORK ERROR] ${response.url()} - ${response.status()} ${response.statusText()}`);
    }
  });

  try {
    await page.goto('http://localhost:3001/login', { waitUntil: 'networkidle0' });
    console.log('--- Page Loaded ---');
    const bodyHTML = await page.evaluate(() => document.body.innerHTML);
    if (bodyHTML.trim().length < 500) {
      console.log('--- Warning: Body is suspiciously small ---');
      console.log(bodyHTML);
    }
  } catch (e) {
    console.log(`[PUPPETEER ERROR] Failed to navigate: ${e.message}`);
  }

  await browser.close();
})();
