async function test() {
  const res = await fetch('http://localhost:3000/api/auth/csrf');
  const data = await res.json();
  const token = data.csrfToken;
  const cookie = res.headers.get('set-cookie').split(';')[0];
  
  const signinRes = await fetch('http://localhost:3000/api/auth/signin/google', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Cookie': cookie
    },
    body: `csrfToken=${token}`,
    redirect: 'manual'
  });
  
  console.log(signinRes.headers.get('location'));
}

test();
