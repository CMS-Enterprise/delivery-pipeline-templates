/** @type {import('next').NextConfig} */
const nextConfig = {
  // The container runtime stage copies .next/standalone, which only this mode emits.
  output: 'standalone',
  // Reproducible builds: the build ID defaults to a random value. Derive it
  // from the commit time the image build supplies; null keeps the default.
  generateBuildId: async () => process.env.SOURCE_DATE_EPOCH ?? null,
};

export default nextConfig;
