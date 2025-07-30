import TagManagement from "../../../components/tag/tag-management"
import NavbarFinal from "../../../components/layout/navbar-final"

export default function TagsPage() {
  return (
    <div className="min-h-screen bg-gray-50">
      <NavbarFinal />
      <div className="container mx-auto px-4 py-6">
        <TagManagement />
      </div>
    </div>
  )
}