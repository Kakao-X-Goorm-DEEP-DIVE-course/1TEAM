import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import '../styles/MainVars.css';
import '../styles/MainStyle.css';

const SearchResultPage = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const query = new URLSearchParams(location.search).get('query');

  const [searchTerm, setSearchTerm] = useState('');
  const [filteredStocks, setFilteredStocks] = useState([]);
  const [stockData, setStockData] = useState({}); // WebSocket에서 받은 실시간 데이터 저장

  const handleSearch = () => {
    if (searchTerm.trim()) {
      navigate(`/search?query=${searchTerm}`);
    }
  };

  useEffect(() => {
    const fetchStockIds = async () => {
      try {
        const response = await fetch(
          `http://${process.env.REACT_APP_STOCK_BACKEND_URL}/stocks/search/${query}`
        );
        if (!response.ok) {
          throw new Error('Stock ID 검색 실패');
        }

        const stockIds = await response.json(); // 주어진 stockId 배열

        console.log(JSON.stringify(stockIds));
        // Backend로 subscriptionList 전달
        await fetch('http://${process.env.REACT_APP_STOCK_BACKEND_URL}/subscriptions/update', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(stockIds),
        });

        const stockDataPromises = stockIds.map(async (stockId) => {
          // 각 stockId에 대한 POST 및 GET 처리
          await fetch(`http://${process.env.REACT_APP_STOCK_BACKEND_URL}/api/daily-price/${stockId}`, {
            method: 'POST',
          });

          const dailyResponse = await fetch(
            `http://${process.env.REACT_APP_STOCK_BACKEND_URL}/api/daily-price/${stockId}`
          );
          if (!dailyResponse.ok) {
            throw new Error(`Daily 데이터 검색 실패 for stockId: ${stockId}`);
          }

          const dailyData = await dailyResponse.json();

          // `date` 기준으로 가장 최근 데이터 선택
          const latestData = dailyData.reduce((latest, current) =>
            current.date > (latest?.date || 0) ? current : latest
          );

          return {
            stockId,
            ...latestData, // 가장 최근 데이터만 사용
          };
        });

        const stockData = await Promise.all(stockDataPromises);
        setFilteredStocks(stockData);
      } catch (error) {
        console.error('검색 데이터 로드 실패:', error);
      }
    };

    if (query) {
      fetchStockIds();
    }
  }, [query]);

  // WebSocket 연결
  useEffect(() => {
    const socket = new WebSocket('ws://localhost:8080/ws/stock');

    socket.onmessage = (event) => {
      const data = JSON.parse(event.data);

      // 실시간 데이터 갱신
      setStockData((prevData) => ({
        ...prevData,
        [data.stockId]: data,
      }));
    };

    socket.onopen = () => {
      console.log('WebSocket 연결 성공');
    };

    socket.onerror = (error) => {
      console.error('WebSocket 에러:', error);
    };

    socket.onclose = () => {
      console.log('WebSocket 연결 종료');
    };

    return () => {
      socket.close();
    };
  }, []);

  const fetchRedisFallback = async (stockId) => {
    try {
      const response = await fetch(
        `http://${process.env.REACT_APP_STOCK_BACKEND_URL}/api/redis-data/${stockId}`
      );
      if (!response.ok) {
        throw new Error(`Redis 데이터 검색 실패 for stockId: ${stockId}`);
      }
      const data = await response.json();

      if (data.length > 0) {
        return JSON.parse(data[0]);
      }
      return null;
    } catch (error) {
      console.error(error);
      return null;
    }
  };

  return (
    <div className="_0-1-home">
      <div className="frame-45">
        {/* Header */}
        <div className="gnb">
          <div className="frame-11">
            <div className="frame-26">
              <img className="image-6" src="/image-60.png" alt="Logo" />
              <div className="frame-10">
                <div className="frame-9">
                  <div
                    className="gnb-button"
                    onClick={() => navigate('/')}
                    style={{ cursor: 'pointer' }}
                  >
                    홈으로
                  </div>
                  <div
                    className="gnb-button"
                    onClick={() => navigate('/login')}
                    style={{ cursor: 'pointer' }}
                  >
                    로그인
                  </div>
                </div>
              </div>
            </div>
            <div className="search-bar">
              <div className="frame-1">
                <img
                  className="search-01"
                  src="/search-010.svg"
                  alt="Search Icon"
                  style={{ cursor: 'pointer' }}
                  onClick={handleSearch}
                />
                <input
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  placeholder="검색하실 종목 이름을 입력해 주세요."
                  className="div2"
                />
              </div>
            </div>
          </div>
        </div>

        <img className="image-9" src="/image-90.png" alt="Main Graphic" />

        <div className="main-content">
          {/* 검색 결과 표시 */}
          <div className="stock-ranking">
            <div className="top-10">
              🔎 다음에 대한 검색 결과 표시 중: {query}
            </div>

            {filteredStocks.length === 0 ? (
              <p>검색 결과가 없습니다.</p>
            ) : (
              <table className="stock-table">
                <thead>
                  <tr>
                    <th>종목이름</th>
                    <th>현재가</th>
                    <th>등락가</th>
                    <th>등락률</th>
                    <th>거래량</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredStocks.map((stock) => {
                    const currentData =
                      stockData[stock.stockId]?.currentPrice || null;

                    if (currentData === null) {
                      fetchRedisFallback(stock.stockId).then((redisData) => {
                        if (redisData) {
                          setStockData((prevData) => ({
                            ...prevData,
                            [stock.stockId]: redisData,
                          }));
                        }
                      });
                    }

                    return (
                      <tr
                        key={stock.stockId}
                        onClick={() => navigate(`/stock/${stock.stockId}`)}
                        style={{ cursor: 'pointer' }}
                      >
                        <td>{stock.stockName}</td>
                        <td>{currentData}</td>
                        <td
                          style={{
                            color:
                              stockData[stock.stockId]?.fluctuationPrice > 0
                                ? '#FF4726'
                                : '#2175F2',
                          }}
                        >
                          {stockData[stock.stockId]?.fluctuationPrice}
                        </td>
                        <td
                          style={{
                            color:
                              parseFloat(
                                stockData[stock.stockId]?.fluctuationRate
                              ) > 0
                                ? '#FF4726'
                                : '#2175F2',
                          }}
                        >
                          {stockData[stock.stockId]?.fluctuationRate}%
                        </td>
                        <td>{stock.volume}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default SearchResultPage;
