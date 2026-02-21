/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // 华为 VMall 风格：主色红、浅灰背景
        'vmall': {
          red: '#C7000B',      // 华为红主色
          'red-hover': '#A00009',
          'gray-bg': '#F5F5F5',
          'gray-text': '#666666',
          'gray-border': '#E0E0E0',
        },
      },
    },
  },
  plugins: [
    require('@tailwindcss/typography'),
  ],
}
