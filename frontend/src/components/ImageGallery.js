import React, { useEffect, useState } from 'react';
import { Box, Heading, Input, Button, Flex, Image, useToast } from '@chakra-ui/react';
import { FaTrash, FaDownload, FaPlus, FaSearch } from 'react-icons/fa';
import { Link } from 'react-router-dom';

const API_BASE = 'http://localhost:8081';

const ImageGallery = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [images, setImages] = useState([]);
  const toast = useToast();

  // Initial load of all images
  useEffect(() => {
    const fetchAll = async () => {
      try {
        const res = await fetch(`${API_BASE}/images`);
        if (!res.ok) throw new Error(`GET /images failed (${res.status})`);
        const data = await res.json();
        setImages(data);
      } catch (err) {
        console.error('Error fetching images:', err);
        toast({
          title: 'Error',
          description: 'Failed to load images.',
          status: 'error',
          duration: 3000,
          isClosable: true,
        });
      }
    };
    fetchAll();
  }, [toast]);

  const handleSearch = async () => {
    if (searchQuery.trim() === '') {
      // reload all
      try {
        const res = await fetch(`${API_BASE}/images`);
        const data = await res.json();
        setImages(data);
      } catch (err) {
        console.error(err);
      }
      return;
    }

    // optional: limit to known colors
    const allowedColors = ['red', 'blue', 'green', 'yellow'];
    if (!allowedColors.includes(searchQuery.toLowerCase())) {
      toast({
        title: 'Invalid Input',
        description: 'Please enter a valid color: red, blue, green, yellow.',
        status: 'warning',
        duration: 3000,
        isClosable: true,
      });
      return;
    }

    try {
      const res = await fetch(`${API_BASE}/images/search?color=${encodeURIComponent(searchQuery)}`);
      if (!res.ok) throw new Error(`GET /images/search failed (${res.status})`);
      const data = await res.json();
      setImages(data);
      toast({
        title: 'Search Result',
        description: `Images with dominant color ${searchQuery} retrieved.`,
        status: 'success',
        duration: 2500,
        isClosable: true,
      });
    } catch (err) {
      console.error('Error searching images:', err);
      toast({
        title: 'Error',
        description: 'Error fetching images.',
        status: 'error',
        duration: 3000,
        isClosable: true,
      });
    }
  };

  const handleDeleteImage = async (imageId) => {
    try {
      const res = await fetch(`${API_BASE}/images/${imageId}`, { method: 'DELETE' });
      if (!res.ok) throw new Error(`DELETE /images/${imageId} failed (${res.status})`);
      toast({
        title: 'Image deleted',
        status: 'success',
        duration: 2000,
        isClosable: true,
      });
      // refresh
      const r = await fetch(`${API_BASE}/images`);
      setImages(await r.json());
    } catch (err) {
      console.error('Error deleting image:', err);
      toast({
        title: 'Error',
        description: 'Error deleting image.',
        status: 'error',
        duration: 3000,
        isClosable: true,
      });
    }
  };

  const handleDownloadImage = async (imageId, imageName) => {
    try {
      const res = await fetch(`${API_BASE}/images/${imageId}/download`, {
        method: 'GET',
        headers: { 'Content-Type': 'application/octet-stream' },
      });
      if (!res.ok) throw new Error(`GET /images/${imageId}/download failed (${res.status})`);

      const blob = await res.blob();
      const url = window.URL.createObjectURL(blob);

      const link = document.createElement('a');
      link.href = url;
      link.download = imageName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Error downloading image:', err);
      toast({
        title: 'Download failed',
        description: 'Could not download the image.',
        status: 'error',
        duration: 2500,
        isClosable: true,
      });
    }
  };

  const handleSearchHexValue = (hexValue) => {
    const encoded = encodeURIComponent(hexValue);
    window.open(`https://www.google.com/search?q=${encoded}`, '_blank');
  };

  return (
    <Flex direction="column" alignItems="center" bg="gray.50" minH="100vh" width="1000px">
      <Box maxW="1200px" width="100%" p="4" mt="4" boxShadow="md" borderRadius="md" bg="white">
        <Flex justifyContent="space-between" alignItems="center" mb="4">
          <Heading as="h1" size="xl">My Gallery</Heading>
          <Link to="/upload">
            <Button colorScheme="green" leftIcon={<FaPlus />} size="sm">
              Add Image
            </Button>
          </Link>
        </Flex>

        <Flex alignItems="center" mb="4">
          <Input
            placeholder="Enter color: red, blue, green, yellow (or leave empty)"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            flex="1"
            mr="2"
          />
          <Button colorScheme="blue" onClick={handleSearch}>Search</Button>
        </Flex>

        <Flex justifyContent="center" flexWrap="wrap">
          {images.map((image) => (
            <Box
              key={image.id}
              width="300px"
              height="300px"
              mr="4"
              mb="4"
              borderRadius="md"
              overflow="hidden"
              boxShadow="md"
              bg="gray.100"
            >
              <a href={image.imageUrl} target="_blank" rel="noopener noreferrer">
                {/* Note: For this to render, S3 object must be public OR switch to backend /download endpoint for <img src> */}
                <Image src={image.imageUrl} alt={image.title} objectFit="cover" width="100%" height="85%" />
              </a>
              <Flex justifyContent="space-between" alignItems="center" p="2">
                <Heading as="h2" size="sm" noOfLines={1}>
                  {image.title}
                </Heading>
                <Button colorScheme="red" size="sm" onClick={() => handleDeleteImage(image.id)} ml="2" title="Delete">
                  <FaTrash />
                </Button>
                <Button colorScheme="blue" size="sm" onClick={() => handleDownloadImage(image.id, image.title)} ml="2" title="Download">
                  <FaDownload />
                </Button>
                <Button colorScheme="purple" size="sm" onClick={() => handleSearchHexValue(image.hexValue)} ml="2" title="Search hex">
                  <FaSearch />
                </Button>
              </Flex>
            </Box>
          ))}
        </Flex>
      </Box>
    </Flex>
  );
};

export default ImageGallery;
