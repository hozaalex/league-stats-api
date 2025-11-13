import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

function SearchForm() {
  const [gameName, setGameName] = useState("");
  const [tag, setTag] = useState("");
  const [region, setRegion] = useState("");
  const navigate = useNavigate();

  const handleSearch = () => {
    if (!gameName.trim()) return alert("Please enter your summoner name");
    if (!tag.trim()) return alert("Please enter your tagline");
    if(!region) return alert("")
    navigate("/search", { state: { gameName, tag, region } });
  };

  return (
    <div className="search-form">
      <input
        type="text"
        placeholder="Game Name"
        value={gameName}
        onChange={(e) => setGameName(e.target.value)}
      />

      <input
        type="text"
        placeholder="Tag"
        value={tag}
        onChange={(e) => setTag(e.target.value)}
      />

      <select value={region} onChange={(e) => setRegion(e.target.value)}>
        <option value="">Select region</option>
        <option value="EUW">EUW</option>
        <option value="EUNE">EUNE</option>
        <option value="NA">NA</option>
        <option value="KR">KR</option>
        <option value="JP">JP</option>
        <option value="BR">BR</option>
        <option value="TR">TR</option>
        <option value="RU">RU</option>
        <option value="OCE">OCE</option>
        <option value="LAN">LAN</option>
        <option value="LAS">LAS</option>
        
      </select>

      <button onClick={handleSearch}>🔍</button>
    </div>
  );
}

export default SearchForm;
