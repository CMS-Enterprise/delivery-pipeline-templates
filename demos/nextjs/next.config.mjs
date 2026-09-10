/** @type {import('next').NextConfig} */
const nextConfig = {
  // The container runtime stage copies .next/standalone, which only this mode emits.
  output: 'standalone',
};

export default nextConfig;
