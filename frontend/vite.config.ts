import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
<<<<<<< Updated upstream
    plugins: [react()],
=======
    plugins: [react(), tailwindcss()],
    build: {
        sourcemap: true,
    },
>>>>>>> Stashed changes
    server: {
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
                secure: false,
            },
        },
    },
})
