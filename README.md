# DocMind AI — Intelligent Document Intelligence & Enterprise RAG Platform

[![Live Demo](https://img.shields.io/badge/Live%20Demo-Vercel-brightgreen?style=for-the-badge&logo=vercel)](https://docmind-frontend-hazel.vercel.app/)
[![API Docs](https://img.shields.io/badge/API%20Docs-Swagger-blue?style=for-the-badge&logo=swagger)](https://docmind-ai-k78v.onrender.com/swagger-ui/index.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.0-green?style=for-the-badge)](https://spring.io/projects/spring-ai)
[![React](https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-pgvector-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://github.com/pgvector/pgvector)
[![Google Gemini](https://img.shields.io/badge/AI-Google%20Gemini-8E75C2?style=for-the-badge&logo=google&logoColor=white)](https://ai.google.dev/)

> **DocMind** is an enterprise-grade Retrieval-Augmented Generation (RAG) platform that enables conversational Q&A, deep semantic search, and document summarization across multi-format documents (PDF, DOCX, TXT, MD, CSV) with citation grounding and zero hallucinations.

---

## 🚀 Live Links

- **Web Application:** [https://docmind-frontend-hazel.vercel.app/](https://docmind-frontend-hazel.vercel.app/)
- **Swagger REST API:** [https://docmind-ai-k78v.onrender.com/swagger-ui/index.html](https://docmind-ai-k78v.onrender.com/swagger-ui/index.html)
- **GitHub Repository:** [https://github.com/NayanExplore/Docmind-AI](https://github.com/NayanExplore/Docmind-AI)

---

## ✨ Key Features

- 📄 **Multi-Format Ingestion:** Ingests and processes PDFs, Word documents, Markdown, Text, and CSV files using Apache Tika and PDFBox readers.
- 🔍 **Vector Embeddings & Semantic Search:** Embeds text chunks into 1,536-dimensional vectors using Google Gemini (`gemini-embedding-001`) and stores them in PostgreSQL with `pgvector` HNSW index for sub-second similarity matching.
- 💬 **Context-Grounded Q&A:** Synthesizes precise, grounded answers with `gemini-3.5-flash` strictly citing source documents, snippets, and page numbers.
- ⚡ **Real-Time Token Streaming:** Delivers live answer streaming via Server-Sent Events (SSE) with progressive markdown rendering and syntax highlighting.
- 🔒 **Multi-Tenant Security:** Full JWT-based stateless authentication with strict user-level document isolation (users can only search and query their own files).
- 🧠 **Multi-Turn Chat Memory:** Retains conversational history across sessions with JpaChatMemory and scoped context tracking.

---

## 🏗️ Architecture

```
┌────────────────────────────────┐         ┌────────────────────────────────┐
│         React + Vite           │  HTTPS  │      Spring Boot 4 Backend     │
│   (TailwindCSS, Zustand, SSE)  │ ──────> │    (Java 21, Spring AI 2.0)    │
│    Hosted on Vercel            │         │      Hosted on Render          │
└────────────────────────────────┘         └───────────────┬────────────────┘
                                                           │
                                   ┌───────────────────────┴───────────────────────┐
                                   │                                               │
                         ┌─────────▼───────────┐                         ┌─────────▼───────────┐
                         │   Google Gemini API │                         │    Neon PostgreSQL  │
                         │ (gemini-3.5-flash & │                         │     with pgvector   │
                         │ gemini-embedding)   │                         │  (HNSW 1536-dim)    │
                         └─────────────────────┘                         └─────────────────────┘
```

---

## 🛠️ Tech Stack

### Backend
- **Framework:** Spring Boot 4.1.0 (Java 21)
- **AI Integration:** Spring AI 2.0.0
- **Vector Database:** PostgreSQL 16 with `pgvector` extension
- **Security:** Spring Security 6 with JWT (stateless authentication)
- **Document Parsers:** Apache Tika Document Reader, Spring AI PDFBox Reader
- **ORM:** Spring Data JPA / Hibernate ORM
- **API Documentation:** SpringDoc OpenAPI 3.1 & Swagger UI

### Frontend
- **Framework:** React 19 + TypeScript + Vite
- **Styling:** Tailwind CSS v4, Lucide Icons
- **State Management:** Zustand
- **Markdown & Code:** React-Markdown, Remark-GFM, PrismJS
- **HTTP & Streaming:** Axios & Fetch Streams API (SSE)

### DevOps & Deployment
- **Backend:** Docker (Multi-stage build) on Render
- **Frontend:** Vercel Global Edge Network
- **Database:** Neon.tech Serverless Cloud PostgreSQL

---

## ⚙️ Local Development Setup

### Prerequisites
- Java 21 JDK (e.g. Eclipse Temurin 21)
- Node.js 20+
- Docker & Docker Compose (for local pgvector)

### 1. Clone the repository
```bash
git clone https://github.com/NayanExplore/Docmind-AI.git
cd Docmind-AI
```

### 2. Start PostgreSQL with pgvector (via Docker)
```bash
docker compose up -d
```
*PostgreSQL will be running on port `5434` with `docmind` database.*

### 3. Run the Backend
```bash
# Set your Gemini API Key
export GEMINI_API_KEY="your_api_key_here"

# On Windows PowerShell:
# $env:GEMINI_API_KEY="your_api_key_here"

./mvnw spring-boot:run
```
*Backend runs on `http://localhost:8081`. Swagger UI is available at `http://localhost:8081/swagger-ui/index.html`.*

### 4. Run the Frontend
```bash
cd frontend/docmind-frontend
npm install
npm run dev
```
*Frontend runs on `http://localhost:3000` and automatically proxies API requests to port `8081`.*

---

## 📌 REST API Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Register a new user |
| `POST` | `/api/v1/auth/login` | Authenticate and obtain JWT access token |
| `POST` | `/api/v1/documents/upload` | Upload & index PDF, Word, TXT, MD, CSV |
| `GET` | `/api/v1/documents/user` | List all documents belonging to current user |
| `DELETE`| `/api/v1/documents/{id}` | Delete document and purge vector embeddings |
| `POST` | `/api/v1/chat/query` | Synchronous grounded Q&A with citations |
| `POST` | `/api/v1/chat/stream` | Real-time SSE token streaming Q&A |
| `POST` | `/api/v1/chat/search/similarity` | Semantic similarity vector search |

---

## 📄 License
This project is licensed under the MIT License.
