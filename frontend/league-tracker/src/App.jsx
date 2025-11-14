import './App.css'
import Navbar from './components/Navbar'
import Header from './components/Header'
import SearchForm from './components/SearchForm'
import StatsPage from './components/StatsPage'

import { BrowserRouter as Router, Routes, Route } from "react-router-dom";

function App() {
  return(
  <Router>
        <div className="main-page">
                <Navbar />
                <Routes>
                        <Route
                                path="/"
                                element={
                                        <main className="content">
                                                <Header />
                                                <SearchForm />
                                        </main>
                                        }
                        />
                        <Route path="/search" element={<StatsPage />} /> 
                
                </Routes>
        </div>
          
  </Router>
          
          )
}

export default App
