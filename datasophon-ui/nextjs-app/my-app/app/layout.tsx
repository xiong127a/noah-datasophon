import { Inter as FontSans } from "next/font/google"
import { cn } from "@/lib/utils"
import "@/app/globals.css"

export const metadata = {
  title: "Datasophon - 大数据平台部署与管理系统",
  description: "一站式大数据平台部署与管理系统",
  icons: {
    icon: "/login-img/logo.svg",
    shortcut: "/login-img/logo.svg",
    apple: "/login-img/logo.svg",
  },
}

const fontSans = FontSans({
  subsets: ["latin"],
  variable: "--font-sans",
})

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="zh-CN">
      <head />
      <body
        className={cn(
          "min-h-screen bg-background font-sans antialiased",
          fontSans.variable
        )}
      >
        {children}
      </body>
    </html>
  )
}
