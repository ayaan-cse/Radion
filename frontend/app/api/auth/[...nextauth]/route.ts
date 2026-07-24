import NextAuth from "next-auth";
import GoogleProvider from "next-auth/providers/google";

const handler = NextAuth({
  providers: [
    GoogleProvider({
      clientId: process.env.GOOGLE_CLIENT_ID!,
      clientSecret: process.env.GOOGLE_CLIENT_SECRET!,
    }),
  ],
  session: { strategy: "jwt" },
  callbacks: {
    async jwt({ token, user, account }) {
      // The user and account objects are only defined on the initial sign-in
      if (account && user) {
        try {
          const res = await fetch(`${process.env.INTERNAL_API_URL}/auth/sync`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              email: user.email,
              firstName: user.name?.split(' ')[0] || '',
              lastName: user.name?.split(' ').slice(1).join(' ') || '',
              avatarUrl: user.image
            })
          });
          
          if (res.ok) {
            const data = await res.json();
            token.id = data.id; // Override token id with backend UUID
          } else {
            console.error("Backend sync failed:", res.status);
          }
        } catch (error) {
          console.error("Error syncing user:", error);
        }
      }
      return token;
    },
    async session({ session, token }) {
      if (session.user) {
        session.user.id = token.id as string;
        // In production, we pass the JWT to Spring Boot for stateless auth
        session.accessToken = token.jti as string; 
      }
      return session;
    },
  },
  pages: {
    signIn: '/login',
  },
});

export { handler as GET, handler as POST };