async function openEditor() {
  fetch('http://13.204.49.246:3000/onlyoffice-config')
    .then(res => res.json())
    .then(config => {
	console.log(config);
     new DocsAPI.DocEditor("onlyoffice-editor", config);
    });
}
async function sendData() {
      const payload = {
		 // fileUrl: "https://dbqualcy.s3.ap-south-1.amazonaws.com/sampledoc.docx",
		  fileUrl: "http://localhost:3000/download",
        user: {
        id: "Subash",
        name: "Subash Rout",
      }
      };

      try {
        const response = await fetch('http://localhost:3000/api/data', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(payload)
        });

        if (!response.ok) throw new Error('Network response was not ok');
        
        const result = await response.json();
        console.log('Server response:', result);
		 new DocsAPI.DocEditor("onlyoffice-editor", result);
      } catch (error) {
        console.error('Error:', error);
      }
    }
