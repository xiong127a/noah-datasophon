import { Inter as FontSans } from "next/font/google"
import { cn } from "@/lib/utils"
import { Toaster } from "sonner"
import "@/app/globals.css"
import "@/styles/apple-toast.css"

export const metadata = {
  title: "Datasophon - 大数据平台部署与管理系统",
  description: "一站式大数据平台部署与管理系统",
  icons: {
    icon: "/images/login/logo.svg",
    shortcut: "/images/login/logo.svg",
    apple: "/images/login/logo.svg",
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
        <Toaster 
          position="top-right"
          expand={true}
          richColors={true}
          closeButton={true}
          duration={4000}
        />
      </body>
    </html>
  )
}
