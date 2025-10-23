"use client"

import * as React from "react"
import { Check } from "lucide-react"
import { cn } from "@/lib/utils"

export interface CheckboxProps {
  checked?: boolean
  onCheckedChange?: (checked: boolean) => void
  disabled?: boolean
  className?: string
  id?: string
}

const Checkbox = React.forwardRef<HTMLButtonElement, CheckboxProps>(
  ({ className, checked = false, onCheckedChange, disabled = false, ...props }, ref) => {
    return (
      <button
        type="button"
        role="checkbox"
        aria-checked={checked}
        disabled={disabled}
        ref={ref}
        className={cn(
          "peer h-4 w-4 shrink-0 rounded-sm border border-slate-300 bg-white ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 transition-all duration-200",
          checked && "bg-blue-600 border-blue-600 text-white",
          className
        )}
        onClick={() => onCheckedChange?.(!checked)}
        {...props}
      >
        <span className="flex items-center justify-center text-current">
          {checked && <Check className="h-3 w-3" />}
        </span>
      </button>
    )
  }
)

Checkbox.displayName = "Checkbox"

export { Checkbox }
