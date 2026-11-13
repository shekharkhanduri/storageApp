import { getFiles, getTotalSpaceUsed } from "@/lib/actions/file.actions";
import DashboardContent from "@/components/dashboard/DashboardContent";

export const dynamic = "force-dynamic";

const Dashboard = async () => {
  // Parallel server requests
  const [filesResult, totalSpace] = await Promise.all([
    getFiles({ types: [], limit: 10 }),
    getTotalSpaceUsed(),
  ]);

  return (
    <DashboardContent
      totalSpace={totalSpace}
      files={filesResult.documents}
      appwriteFiles={filesResult.appwrite.documents}
      minioFiles={filesResult.minio.documents}
    />
  );
};

export default Dashboard;
